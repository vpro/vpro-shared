package nl.vpro.hibernate.search6;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michiel Meeuwissen
 * @since 1.10
 */
public abstract class IterableToStringBridge<T> implements ValueBridge<Iterable<T>, List<String>> {

    @Override
    public List<String> toIndexedValue(Iterable<T> value, ValueBridgeToIndexedValueContext valueBridgeToIndexedValueContext) {
        if (value != null) {
            List<String> result = new ArrayList<>();
            for (T object : value) {
                if (object != null) {
                    result.add(toString(object));
                }
            }
            return result;
        } else {
            return null;
        }
    }


    protected abstract String toString(T object);
}
