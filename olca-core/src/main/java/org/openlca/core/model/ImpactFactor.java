package org.openlca.core.model;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "tbl_impact_factors")
public class ImpactFactor extends AbstractEntity implements Cloneable {

	@OneToOne
	@JoinColumn(name = "f_flow")
	public Flow flow;

	@OneToOne
	@JoinColumn(name = "f_flow_property_factor")
	public FlowPropertyFactor flowPropertyFactor;

	@OneToOne
	@JoinColumn(name = "f_unit")
	public Unit unit;

	@Column(name = "value")
	public double value = 1;

	@Column(name = "formula")
	public String formula;

	@Embedded
	public Uncertainty uncertainty;

	@OneToOne
	@JoinColumn(name = "f_location")
	public Location location;

	public static ImpactFactor of(Flow flow, double value) {
		var f = new ImpactFactor();
		f.value = value;
		if (flow != null) {
			f.flow = flow;
			f.flowPropertyFactor = flow.getReferenceFactor();
			f.unit = flow.getReferenceUnit();
		}
		return f;
	}

	@Override
	public ImpactFactor clone() {
		var clone = new ImpactFactor();
		clone.flow = flow;
		clone.flowPropertyFactor = flowPropertyFactor;
		clone.unit = unit;
		clone.value = value;
		clone.formula = formula;
		if (uncertainty != null) {
			clone.uncertainty = uncertainty.clone();
		}
		clone.location = location;
		return clone;
	}

}
