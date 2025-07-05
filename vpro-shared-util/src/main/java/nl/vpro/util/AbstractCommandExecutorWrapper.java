package nl.vpro.util;

import java.util.function.Supplier;

import org.meeuw.functional.Unwrappable;

import nl.vpro.logging.simple.SimpleLogger;

/**
 * Implementation of {@link CommandExecutor} wrapping another one. The idea is that this can be
 * extended to add useful extra methods related to the wrapped command, while the command itself
 * can be instantiated with {@link CommandExecutorImpl#builder()}
 * @since 5.4
 */
public abstract class AbstractCommandExecutorWrapper implements CommandExecutor, Unwrappable<CommandExecutor> {


    public AbstractCommandExecutorWrapper() {

    }


    protected abstract CommandExecutor getWrapped();

    @Override
    public int execute(String... args) {
        return getWrapped().execute(args);
    }

    @Override
    public int execute(Parameters parameters) {
        return getWrapped().execute(parameters);
    }

    @Override
    public SimpleLogger getLogger() {
        return getWrapped().getLogger();
    }

    @Override
    public Supplier<String> getBinary() {
        return getWrapped().getBinary();
    }

    @Override
    public CommandExecutor unwrap() {
        return getWrapped();
    }
}
