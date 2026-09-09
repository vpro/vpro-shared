package nl.vpro.util;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.*;


/**
 * Given some API which supplies only 'batched' retrieval (so e.g. with offset and max/batchsize parameters),
 * access such an API as an iterator to visit all elements.
 * <p>
 * If an API provides access to huge set of elements, they often do it with some paging mechanism, or by some 'resumption token' formalism. With {@link BatchedReceiver} this can be morphed into a simple {@link java.util.Iterator}.
 *
 *
 * <h3>Paging</h3>
 * The 'batchGetter' argument should be a {@link java.util.function.BiFunction}, returning an iterator for the page described by given offset and batch size
 * <pre>
 * {@code
 * Iterator<String> i = BatchedReceiver.<String>builder()
 *     .batchGetter((offset, max) ->
 *        apiClient.getPage(offset, max).iterator()
 *     )
 *     .batchSize(6)
 *     .build();
 * i.forEachRemaining(string -> {
 *       ...<do stuff...>
 *   });
 * }</pre>
 * <h3>Resumption token formalism</h3>
 * You simply provide a {@link java.util.function.Supplier}. A lambda would probably not suffice because you might need the previous result the get the next one. E.g. this (using olingo code)
 * <h4>Just use a supplier</h4>
 * <pre>
 * {@code
 *    public Iterator<ClientEntity> iterate(URIBuilder ub) {
 *         return BatchedReceiver.<ClientEntity>builder()
 *             .batchGetter(new Supplier<Iterator<ClientEntity>>() {
 *                 ClientEntitySet result;
 *                 @Override
 *                 public Iterator<ClientEntity> get() {
 *                     if (result != null) {
 *                         result = query(result.getNext());
 *                     } else {
 *                         result = query(ub);
 *                     }
 *                     return result.getEntities().iterator();
 *                 }
 *             })
 *             .build();
 *     }
 * }
 * </pre>
 * <h4>An initial supplier and a 'next page'</h4>
 * This case could actually be simplified like so
 * <pre>
 *  {@code
 *      public Iterator<ClientEntity> iterate(URIBuilder ub) {
 *           return BatchedReceiver.<ClientEntity>builder()
 *               .initialAndResumption(
 *                     () -> query(ub),
 *                     (result) -> query(result.getNext()),
 *                     (result) -> result.getEntities().iterator()
 *               .build();
 *       }
 *   }
 * </pre>
 * If the result is an {@link Iterable} itself, it can be
 * <pre>
 * @{code
 *  public Iterator<ClientEntity> iterate(URIBuilder ub) {
 *             return BatchedReceiver.<ClientEntity>builder()
 *                 .initialAndResumption(
 *                       () -> query(ub),
 *                       (result) -> query(result.getNext())
 *                 .build();
 * }
 * </pre>
 *
 *
 *
 * @author Michiel Meeuwissen
 * @since 1.68
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@ToString
@Slf4j
@Deprecated
public class BatchedReceiver<T> extends org.meeuw.collections.BatchedReceiver<T> {


    @lombok.Builder(
        builderClassName = "Builder",
        buildMethodName = "_build")
    private BatchedReceiver(
        Long offset,
        Supplier<Optional<Iterator<T>>> supplier) {
        super(offset, supplier);
    }


}
