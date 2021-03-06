package org.openlca.jsonld.input;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.jsonld.Json;

import com.google.gson.JsonObject;

class ParameterImport extends BaseImport<Parameter> {

	ParameterImport(String refId, ImportConfig conf) {
		super(ModelType.PARAMETER, refId, conf);
	}

	static Parameter run(String refId, ImportConfig conf) {
		return new ParameterImport(refId, conf).run();
	}

	@Override
	Parameter map(JsonObject json, long id) {
		if (json == null)
			return null;
		Parameter p = new Parameter();
		In.mapAtts(json, p, id, conf); // TODO <- mapAtts
		mapFields(json, p);
		return conf.db.put(p);
	}

	/** Field mappings for processes and LCIA categories. */
	static void mapFields(JsonObject json, Parameter p) {
		In.mapAtts(json, p, p.id); // TODO <- mapAtts
		p.scope = Json.getEnum(json, "parameterScope", ParameterScope.class);
		p.isInputParameter = Json.getBool(json, "inputParameter", true);
		p.value = Json.getDouble(json, "value", 0);
		p.formula = Json.getString(json, "formula");
		p.uncertainty = Uncertainties.read(Json.getObject(json, "uncertainty"));
	}

}
