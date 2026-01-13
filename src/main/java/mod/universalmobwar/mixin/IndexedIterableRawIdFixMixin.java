package mod.universalmobwar.mixin;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.IndexedIterable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Global compatibility fix for registry-entry serialization.
 *
 * Some mods (UMW included) may end up with ItemStacks whose enchantments component contains
 * {@link RegistryEntry.Reference} instances that are valid but originate from a different
 * registry wrapper than the one used by the networking buffer.
 *
 * When another mod (e.g. AdvancedLootInfo + Polymer) encodes an ItemStack during world load,
 * Minecraft may call {@link IndexedIterable#getRawIdOrThrow(Object)} and throw:
 * "Can't find id for 'Reference{...}'".
 *
 * This mixin intercepts that method and, if the raw id lookup fails for a reference entry,
 * attempts to resolve an equivalent reference by {@link RegistryKey} against the current
 * iterable implementation before retrying.
 */
@Mixin(IndexedIterable.class)
public interface IndexedIterableRawIdFixMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getRawIdOrThrow(Ljava/lang/Object;)I", at = @At("HEAD"), cancellable = true)
    private void umw$fixForeignRegistryEntry(Object value, CallbackInfoReturnable<Integer> cir) {
        IndexedIterable self = (IndexedIterable) (Object) this;

        // Fast path: if this iterable can already resolve it, do nothing.
        Integer rawId = tryGetRawId(self, value);
        if (rawId != null && rawId.intValue() >= 0) {
            cir.setReturnValue(rawId);
            return;
        }

        if (!(value instanceof RegistryEntry.Reference<?> reference)) {
            return;
        }

        RegistryKey<?> key;
        try {
            key = reference.getKey().orElse(null);
        } catch (Exception ignored) {
            key = null;
        }
        if (key == null) {
            return;
        }

        RegistryEntry.Reference<?> resolved = tryResolveReference(self, key);
        if (resolved == null) {
            return;
        }

        Integer resolvedRawId = tryGetRawId(self, resolved);
        if (resolvedRawId != null && resolvedRawId.intValue() >= 0) {
            cir.setReturnValue(resolvedRawId);
        }
    }

    @SuppressWarnings({"rawtypes"})
    private static Integer tryGetRawId(IndexedIterable self, Object value) {
        if (self == null || value == null) {
            return null;
        }
        try {
            // IndexedIterable has a getRawId(T) method; use reflection to avoid signature churn.
            Method method = self.getClass().getMethod("getRawId", Object.class);
            Object result = method.invoke(self, value);
            if (result instanceof Integer i) {
                return i;
            }
        } catch (NoSuchMethodException ignored) {
            // Some impls declare getRawId with the generic erasure but not Object.class param;
            // fall through to the untyped search.
        } catch (Exception ignored) {
            return null;
        }

        // Fallback: find any getRawId(*) method returning int.
        try {
            for (Method m : self.getClass().getMethods()) {
                if (!"getRawId".equals(m.getName())) {
                    continue;
                }
                if (m.getParameterCount() != 1 || m.getReturnType() != int.class) {
                    continue;
                }
                Object result = m.invoke(self, value);
                return (Integer) result;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    @SuppressWarnings({"rawtypes"})
    private static RegistryEntry.Reference<?> tryResolveReference(IndexedIterable self, RegistryKey<?> key) {
        if (self == null || key == null) {
            return null;
        }

        // Many registry wrappers used in networking expose getEntry(RegistryKey) -> Optional.
        for (String methodName : new String[] {"getEntry", "getOptional", "get", "getOrEmpty"}) {
            try {
                Method method = self.getClass().getMethod(methodName, RegistryKey.class);
                if (!Optional.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                Object result = method.invoke(self, key);
                if (!(result instanceof Optional<?> optional) || optional.isEmpty()) {
                    continue;
                }
                Object entry = optional.get();
                if (entry instanceof RegistryEntry.Reference<?> reference) {
                    return reference;
                }
            } catch (NoSuchMethodException ignored) {
                // try next
            } catch (Exception ignored) {
                // fall through
            }
        }

        // Fallback: handle Registry$1 (anonymous inner class capturing the outer Registry).
        try {
            Registry<?> outerRegistry = tryGetOuterRegistry(self);
            if (outerRegistry != null) {
                Optional<? extends RegistryEntry.Reference<?>> resolved = outerRegistry.getEntry((RegistryKey) key);
                return resolved.orElse(null);
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private static Registry<?> tryGetOuterRegistry(IndexedIterable self) {
        try {
            for (Field field : self.getClass().getDeclaredFields()) {
                if (!Registry.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(self);
                if (value instanceof Registry<?> registry) {
                    return registry;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
