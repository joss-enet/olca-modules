package org.openlca.core.database.references;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Source;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;

public class ImpactMethodReferenceSearch extends BaseParametrizedReferenceSearch<ImpactMethodDescriptor> {

	private final static Ref[] references = {
			new Ref(Category.class, "category", "f_category", true),
			new Ref(Actor.class, "author", "f_author", true),
			new Ref(Actor.class, "generator", "f_generator", true)
	};
	private final static Ref[] categoryReferences = {
			new Ref(ImpactCategory.class, "id", "id")
	};
	private final static Ref[] factorReferences = {
			new Ref(Flow.class, "flow", ImpactFactor.class, "impactFactors", "f_flow"),
			new Ref(FlowPropertyFactor.class, "flowPropertyFactor", ImpactFactor.class, "impactFactors", "f_flow_property_factor"),
			new Ref(Unit.class, "unit", ImpactFactor.class, "impactFactors", "f_unit")
	};
	private final static Ref[] propertyFactorReferences = {
			new Ref(FlowProperty.class, "flowProperty", FlowPropertyFactor.class, "flowPropertyFactor", "f_flow_property")
	};
	private final static Ref[] sourceReferences = {
			new Ref(Source.class, "sources", "f_source", true)
	};


	public ImpactMethodReferenceSearch(IDatabase database, boolean includeOptional) {
		super(database, ImpactMethod.class, includeOptional);
	}

	@Override
	public List<Reference> findReferences(Set<Long> ids) {
		List<Reference> results = new ArrayList<>();
		results.addAll(findReferences("tbl_impact_methods", "id", ids, references));
		results.addAll(findFactorReferences(ids));
		results.addAll(findParameters(ids, getFactorFormulas(ids)));
		results.addAll(findReferences("tbl_source_links", "f_owner", ids, sourceReferences));
		return results;
	}

	private List<Reference> findFactorReferences(Set<Long> ids) {
		List<Reference> results = new ArrayList<>();
		Map<Long, Long> categories = toIdMap(findReferences("tbl_impact_categories", "f_impact_method", ids, categoryReferences));
		Map<Long, Long> factors = toIdMap(findReferences("tbl_impact_factors", "f_impact_category", categories.keySet(),
				new Ref[] { new Ref(ImpactFactor.class, "id", "id") }));
		Map<Long, Long> map = new HashMap<>();
		for (Long factor : factors.keySet())
			map.put(factor, categories.get(factors.get(factor)));
		results.addAll(findReferences("tbl_impact_factors", "id", map.keySet(), map, factorReferences));
		List<Reference> propertyFactors = new ArrayList<>();
		for (Reference ref : results) {
			if (ref.getType() != FlowPropertyFactor.class)
				continue;
			propertyFactors.add(ref);
		}
		Map<Long, Long> factorIds = toIdMap(propertyFactors);
		results.addAll(findReferences("tbl_flow_property_factors", "id", factorIds.keySet(), factorIds, propertyFactorReferences));
		return results;
	}

	private Map<Long, Set<String>> getFactorFormulas(Set<Long> ids) {
		String select = "SELECT f_impact_method, formula FROM tbl_impact_factors "
				+ "INNER JOIN tbl_impact_categories "
				+ "ON tbl_impact_categories.id = tbl_impact_factors.f_impact_category ";
		Map<Long, Set<String>> formulas = new HashMap<>();
		List<String> queries = Search.createQueries(select, "WHERE f_impact_method IN", ids);
		for (String query : queries) {
			Search.on(database, null).query(query, (result) -> {
				long methodId = result.getLong(1);
				String formula = result.getString(2);
				if (formula != null && !formula.trim().isEmpty()) {
					put(methodId, formula, formulas);
				}
			});
		}
		return formulas;
	}

}
