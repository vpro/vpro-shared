package nl.vpro.util;

import java.util.function.Supplier;

import nl.vpro.logging.simple.SimpleLogger;

/**
 * Implementation of {@link CommandExecutor} wrapping another one. The idea is that this can be
 * extended to add useful extra methods related to the wrapped command, while the command itself
 * can be instantiated with {@link CommandExecutorImpl#builder()}
 * @since 5.4
 */
public class CommandExecutorWrapper implements CommandExecutor {

    final CommandExecutor wrapped;

    public CommandExecutorWrapper(CommandExecutor wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public int execute(String... args) {
        return wrapped.execute(args);
    }

    @Override
    public int execute(Parameters parameters) {
        return wrapped.execute(parameters);
    }

    @Override
    public SimpleLogger getLogger() {
        return wrapped.getLogger();
    }

    @Override
    public Supplier<String> getBinary() {
        return wrapped.getBinary();
    }
}
