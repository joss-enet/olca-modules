package org.openlca.core.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "tbl_impact_categories")
public class ImpactCategory extends ParameterizedEntity {

	@OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
	@JoinColumn(name = "f_impact_category")
	public final List<ImpactFactor> impactFactors = new ArrayList<>();

	@Column(name = "reference_unit")
	public String referenceUnit;

	public static ImpactCategory of(String name) {
		return of(name, null);
	}

	public static ImpactCategory of(String name, String refUnit) {
		var impact = new ImpactCategory();
		Entities.init(impact, name);
		impact.referenceUnit = refUnit;
		return impact;
	}

	@Override
	public ImpactCategory clone() {
		var clone = new ImpactCategory();
		Entities.copyFields(this, clone);
		clone.referenceUnit = referenceUnit;
		for (ImpactFactor f : impactFactors) {
			clone.impactFactors.add(f.clone());
		}
		for (Parameter p : parameters) {
			clone.parameters.add(p.clone());
		}
		return clone;
	}

	public ImpactFactor getFactor(Flow flow) {
		if (flow == null)
			return null;
		for (ImpactFactor factor : impactFactors)
			if (flow.equals(factor.flow))
				return factor;
		return null;
	}

	public ImpactFactor getFactor(String refId) {
		if (refId == null)
			return null;
		for (ImpactFactor factor : impactFactors)
			if (factor.flow != null && refId.equals(factor.flow.refId))
				return factor;
		return null;
	}

	/**
	 * Adds a new characterization factor for the given flow and value. The unit
	 * and flow property are initialized with the respective reference values of
	 * the flow.
	 */
	public ImpactFactor factor(Flow flow, double value) {
		var f = ImpactFactor.of(flow, value);
		impactFactors.add(f);
		return f;
	}

	@Override
	public final ParameterScope parameterScope() {
		return ParameterScope.IMPACT;
	}
}
