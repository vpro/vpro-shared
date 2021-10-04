package nl.vpro.elasticsearch;

public class WrongCluster extends IllegalStateException {
    public WrongCluster(String s) {
        super(s);
    }
}
