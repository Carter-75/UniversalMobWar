package mod.universalmobwar.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiConsumer;

public class GameRulesAccessor {
	@Mixin(GameRules.BooleanRule.class)
	public interface BooleanRuleInvoker {
		@Invoker("create")
		static GameRules.Type<GameRules.BooleanRule> invokeCreate(boolean initialValue, BiConsumer<MinecraftServer, GameRules.BooleanRule> changeCallback) {
			throw new AssertionError();
		}
	}
	
	@Mixin(GameRules.IntRule.class)
	public interface IntRuleInvoker {
		@Invoker("create")
		static GameRules.Type<GameRules.IntRule> invokeCreate(int initialValue, BiConsumer<MinecraftServer, GameRules.IntRule> changeCallback) {
			throw new AssertionError();
		}
	}
	
	@Mixin(GameRules.class)
	public interface GameRulesInvoker {
		@Invoker("register")
		static <T extends GameRules.Rule<T>> GameRules.Key<T> invokeRegister(String name, GameRules.Category category, GameRules.Type<T> type) {
			throw new AssertionError();
		}
	}
}

