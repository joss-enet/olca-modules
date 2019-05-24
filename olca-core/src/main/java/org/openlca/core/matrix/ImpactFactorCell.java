package org.openlca.core.matrix;

import org.openlca.core.math.NumberGenerator;
import org.openlca.core.model.UncertaintyType;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.Scope;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The cell value is negative if the factor is related to an input flow.
 */
class ImpactFactorCell {

	private final long methodId;
	private final boolean inputFlow;
	private final CalcImpactFactor factor;
	private NumberGenerator generator;

	ImpactFactorCell(CalcImpactFactor factor, long methodId,
			boolean inputFlow) {
		this.factor = factor;
		this.methodId = methodId;
		this.inputFlow = inputFlow;
	}

	void eval(FormulaInterpreter interpreter) {
		if (interpreter == null)
			return;
		if (Strings.nullOrEmpty(factor.getAmountFormula()))
			return;

		try {
			Scope scope = interpreter.getScope(methodId);
			if (scope == null) {
				scope = interpreter.getGlobalScope();
			}
			double v = scope.eval(factor.getAmountFormula());
			factor.setAmount(v);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Formula evaluation failed, impact factor " + factor, e);
		}
	}

	double getMatrixValue() {
		if (factor == null)
			return 0;
		double amount = factor.getAmount() * factor.getConversionFactor();
		return inputFlow ? -amount : amount;
	}

	double getNextSimulationValue() {
		UncertaintyType type = factor.getUncertaintyType();
		if (type == null || type == UncertaintyType.NONE)
			return getMatrixValue();
		if (generator == null)
			generator = createGenerator(type);
		double amount = generator.next() * factor.getConversionFactor();
		return inputFlow ? -amount : amount;
	}

	private NumberGenerator createGenerator(UncertaintyType type) {
		final CalcImpactFactor f = factor;
		switch (type) {
		case LOG_NORMAL:
			return NumberGenerator.logNormal(f.getParameter1(),
					f.getParameter2());
		case NORMAL:
			return NumberGenerator.normal(f.getParameter1(), f.getParameter2());
		case TRIANGLE:
			return NumberGenerator.triangular(f.getParameter1(),
					f.getParameter2(), f.getParameter3());
		case UNIFORM:
			return NumberGenerator
					.uniform(f.getParameter1(), f.getParameter2());
		default:
			return NumberGenerator.discrete(f.getAmount());
		}
	}

}
