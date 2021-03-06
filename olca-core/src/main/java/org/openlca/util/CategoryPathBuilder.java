package org.openlca.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;

/**
 * A utility class for building `/` separated category paths for given category
 * IDs. It manages an internal cache and, thus, is fast when using it for many
 * requests.
 */
public class CategoryPathBuilder {

	private final HashMap<Long, Long> parents = new HashMap<>();
	private final HashMap<Long, String> names = new HashMap<>();
	private final HashMap<Long, Object> cache = new HashMap<>();

	public CategoryPathBuilder(IDatabase db) {
		String sql = "select id, name, f_category from tbl_categories";
		try {
			NativeSql.on(db).query(sql, r -> {
				long id = r.getLong(1);
				names.put(id, r.getString(2));
				long parent = r.getLong(3);
				if (!r.wasNull()) {
					parents.put(id, parent);
				}
				return true;
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the category path for the given ID. Passing `null` into this
	 * function is allowed. It will return also `null` in this case as well as
	 * when there is no category with the given ID in the database.
	 */
	public String path(Long id) {
		if (id == null)
			return null;

		// check the cache
		var cached = cache.get(id);
		if (cached instanceof String)
			return (String) cached;

		// build and cache the path
		var path = new StringBuilder();
		long pid = id;
		while (true) {
			var name = names.get(pid);
			if (name == null)
				break;
			if (path.length() > 0) {
				path.insert(0, '/');
			}
			path.insert(0, name.trim());
			Long parent = parents.get(pid);
			if (parent == null)
				break;
			pid = parent;
		}
		var p = path.toString();
		cache.put(id, p);

		return p;
	}

	@SuppressWarnings("unchecked")
	public List<String> list(Long id) {
		if (id == null)
			return Collections.emptyList();

		// check the cache
		var cached = cache.get(id);
		if (cached instanceof List)
			return (List<String>) cached;

		var path = new ArrayList<String>();
		long pid = id;
		while (true) {
			var name = names.get(pid);
			if (name == null)
				break;
			path.add(0, name);
			var parent = parents.get(pid);
			if (parent == null)
				break;
			pid = parent;
		}

		cache.put(id, path);
		return path;
	}
}
