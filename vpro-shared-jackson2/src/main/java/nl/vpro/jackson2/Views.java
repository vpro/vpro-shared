package nl.vpro.jackson2;

@Deprecated(forRemoval = true, since = "use nl.vpro.jackson.Views instead")
public class Views extends nl.vpro.jackson.Views {

    public interface Normal extends nl.vpro.jackson.Views.Normal {
    }

    public interface Forward extends nl.vpro.jackson.Views.Forward {
    }

    public interface Publisher extends nl.vpro.jackson.Views.Publisher {
    }

    public interface ForwardPublisher extends nl.vpro.jackson.Views.ForwardPublisher, Publisher, Forward {
    }
}
