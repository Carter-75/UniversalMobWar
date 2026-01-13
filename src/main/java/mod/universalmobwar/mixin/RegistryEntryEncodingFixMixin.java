package mod.universalmobwar.mixin;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.IndexedIterable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Compatibility fix for cases where another mod constructs ItemStacks containing
 * {@link RegistryEntry.Reference} instances originating from a different registry wrapper.
 *
 * During packet encoding, Minecraft encodes reference entries by raw id via
 * {@link IndexedIterable#getRawIdOrThrow(Object)}. If the reference object isn't the exact
 * instance held by the current buffer's registry wrapper, encoding throws:
 * "Can't find id for 'Reference{...}'".
 *
 * This mixin redirects the raw-id lookup and, on failure, attempts to resolve an equivalent
 * reference entry by {@link RegistryKey} from the current wrapper before retrying.
 */
@Mixin(targets = "net.minecraft.network.codec.PacketCodecs$16")
public final class RegistryEntryEncodingFixMixin {

    @Redirect(
        method = "encode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/collection/IndexedIterable;getRawIdOrThrow(Ljava/lang/Object;)I"
        ),
        require = 0
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private int umw$resolveRawIdOrThrow(IndexedIterable iterable, Object value) {
        try {
            return iterable.getRawIdOrThrow(value);
        } catch (IllegalArgumentException original) {
            if (!(value instanceof RegistryEntry.Reference<?> reference)) {
                throw original;
            }

            RegistryKey<?> key;
            try {
                key = reference.getKey().orElse(null);
            } catch (Exception ignored) {
                key = null;
            }
            if (key == null) {
                throw original;
            }

            RegistryEntry.Reference<?> resolved = tryResolveReference(iterable, key);
            if (resolved == null) {
                throw original;
            }

            return iterable.getRawIdOrThrow(resolved);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RegistryEntry.Reference<?> tryResolveReference(IndexedIterable iterable, RegistryKey<?> key) {
        if (iterable == null || key == null) {
            return null;
        }

        // The concrete iterable used by the play-phase buffer is typically a registry wrapper
        // that exposes a "getEntry(RegistryKey)" or similar method returning Optional.
        // Use reflection to stay resilient to minor mapping / API differences.
        for (String methodName : new String[] {"getEntry", "getOptional", "get", "getOrEmpty"}) {
            try {
                Method method = iterable.getClass().getMethod(methodName, RegistryKey.class);
                if (!Optional.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                Object result = method.invoke(iterable, key);
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

        return null;
    }
}
