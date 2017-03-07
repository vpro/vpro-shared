package nl.vpro.api.client.resteasy;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.cache.Cache;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.client.jaxrs.cache.BrowserCache;
import org.jboss.resteasy.client.jaxrs.cache.CacheEntry;

/**
 * A resteasy {@link org.jboss.resteasy.client.jaxrs.cache.BrowserCache} backend by a {@link javax.cache.Cache}
 * @author Michiel Meeuwissen
 * @since 1.65
 */
public class JavaBrowserCache implements BrowserCache  {

    private final Cache<String, Map<String, Entry>> backing;

    public JavaBrowserCache(Cache<String, Map<String, Entry>> backing) {
        this.backing = backing;
    }


    @Override
    public Entry getAny(String key) {
        Map<String, Entry> parent = backing.get(key);
        if (parent == null) {
            return null;
        }
        Iterator<Entry> iterator = parent.values().iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    @Override
    public Entry get(String key, MediaType accept) {
        Map<String, Entry> parent = backing.get(key);

        if (parent == null || parent.isEmpty()) {
            return null;
        }
        if (accept.isWildcardType()) {
            // if the client accepts */*, return just the first entry for requested URL
            return parent.get(parent.keySet().iterator().next());
        } else if (accept.isWildcardSubtype()) {
            // if the client accepts <media>/*, return the first entry which media type starts with <media>/
            for (Map.Entry<String, Entry> parentEntry : parent.entrySet()) {
                if (parentEntry.getKey().startsWith(accept.getType() + "/")) {
                    return parentEntry.getValue();
                }
            }
        }
        return parent.get(accept.toString());
    }

    Entry put(CacheEntry cacheEntry) {
        Map<String, Entry> map = backing.get(cacheEntry.getKey());
        if (map == null) {
            map = new ConcurrentHashMap<>();
        }
        map.put(cacheEntry.getMediaType().toString(), cacheEntry);
        backing.put(cacheEntry.getKey(), map);
        return cacheEntry;
    }

    @Override
    public Entry put(
        String key, MediaType mediaType,
        MultivaluedMap<String, String> headers, byte[] cached, int expires,
        String etag, String lastModified) {
        return put(new CacheEntry(key, headers, cached, expires, etag, lastModified, mediaType));
    }

    @Override
    public Entry remove(String key, MediaType type) {
        Map<String, Entry> data = backing.get(key);
        if (data == null) return null;
        Entry removed = data.remove(type.toString());
        if (data.isEmpty()) {
            backing.remove(key);
        } else {
            backing.put(key, data);
        }
        return removed;
    }

    @Override
    public void clear() {
        backing.clear();
    }

}
