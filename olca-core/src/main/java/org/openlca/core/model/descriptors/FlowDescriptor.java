package org.openlca.core.model.descriptors;

import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;

public class FlowDescriptor extends CategorizedDescriptor {

	/** An optional location code of the flow. */
	public String location;
	public FlowType flowType;
	public long refFlowPropertyId;

	public FlowDescriptor() {
		this.type = ModelType.FLOW;
	}

}
