package org.openlca.core.results.solutions;

import org.openlca.core.matrix.DIndex;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.matrix.TechIndex;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

public interface SolutionProvider {

	/**
	 * The index $\mathit{Idx}_A$ of the technology matrix $\mathbf{A}$. It maps the
	 * process-product pairs (or process-waste pairs) $\mathit{P}$ of the product
	 * system to the respective $n$ rows and columns of $\mathbf{A}$. If the product
	 * system contains other product systems as sub-systems, these systems are
	 * handled like processes and are also mapped as pair with their quantitative
	 * reference flow to that index (and also their processes etc.).
	 * <p>
	 * $$\mathit{Idx}_A: \mathit{P} \mapsto [0 \dots n-1]$$
	 */
	TechIndex techIndex();

	/**
	 * The row index $\mathit{Idx}_B$ of the intervention matrix $\mathbf{B}$. It
	 * maps the (elementary) flows $\mathit{F}$ of the processes in the product
	 * system to the $k$ rows of $\mathbf{B}$.
	 * <p>
	 * $$\mathit{Idx}_B: \mathit{F} \mapsto [0 \dots k-1]$$
	 */
	FlowIndex flowIndex();

	/**
	 * The row index $\mathit{Idx}_C$ of the matrix with the characterization
	 * factors $\mathbf{C}$. It maps the LCIA categories $\mathit{C}$ to the $l$
	 * rows of $\mathbf{C}$.
	 * <p>
	 * $$\mathit{Idx}_C: \mathit{C} \mapsto [0 \dots l-1]$$
	 */
	DIndex<ImpactCategoryDescriptor> impactIndex();

	/**
	 * The scaling vector $\mathbf{s}$ which is calculated by solving the
	 * equation
	 * <p>
	 * $$\mathbf{A} \ \mathbf{s} = \mathbf{f}$$
	 * <p>
	 * where $\mathbf{A}$ is the technology matrix and $\mathbf{f}$ the final
	 * demand vector of the product system.
	 */
	double[] scalingVector();

	default double scalingFactorOf(int product) {
		var s = scalingVector();
		return empty(s)
				? 0
				: s[product];
	}

	/**
	 * The total requirements of the products to fulfill the demand of the
	 * product system. As our technology matrix $\mathbf{A}$ is indexed
	 * symmetrically (means rows and columns refer to the same process-product
	 * pair) our product amounts are on the diagonal of the technology matrix
	 * $\mathbf{A}$ and the total requirements can be calculated by the
	 * following equation where $\mathbf{s}$ is the scaling vector ($\odot$
	 * denotes element-wise multiplication):
	 * <p>
	 * $$\mathbf{t} = \text{diag}(\mathbf{A}) \odot \mathbf{s}$$
	 */
	default double[] totalRequirements() {
		var index = techIndex();
		var t = new double[index.size()];
		for (int i = 0; i < t.length; i++) {
			t[i] = scaledTechValueOf(i, i);
		}
		return t;
	}

	default double totalRequirementsOf(int product) {
		var t = totalRequirements();
		return empty(t)
				? 0
				: t[product];
	}

	/**
	 * Get the unscaled column $j$ from the technology matrix $A$.
	 */
	double[] techColumnOf(int product);

	/**
	 * Get the unscaled value $a_{ij}$ from the technology matrix $A$.
	 */
	default double techValueOf(int row, int col) {
		double[] column = techColumnOf(col);
		return column[row];
	}

	/**
	 * Get the scaled value $s_j * a_{ij}$ of the technology matrix $A$. On
	 * the diagonal of $A$ these are the total requirements of the system.
	 */
	default double scaledTechValueOf(int row, int col) {
		var s = scalingVector();
		if (s == null)
			return 0;
		var aij = techValueOf(row, col);
		return s[col] * aij;
	}

	/**
	 * Get the scaling vector of the product system for one unit of output (input)
	 * of product (waste flow) j. This is equivalent with the jth column of the
	 * inverse technology matrix $A^{-1}[:,j]$ in case of full in-memory matrices.
	 */
	double[] solutionOfOne(int product);

	/**
	 * The loop factor $loop_j$ of a product $i$ is calculated via:
	 * <p>
	 * $$
	 * loop_j = \frac{1}{\mathbf{A}_{jj} \ \mathbf{A}^{-1}_{jj}}
	 * $$
	 * <p>
	 * It is $1.0$ if the process of the product is not in a loop. Otherwise
	 * it describes ...
	 */
	double loopFactorOf(int product);

	default double totalFactorOf(int product) {
		var t = totalRequirementsOf(product);
		var loop = loopFactorOf(product);
		return loop * f;
	}

	/**
	 * Get the unscaled column $j$ from the intervention matrix $B$.
	 */
	double[] unscaledFlowsOf(int product);

	/**
	 * Get the unscaled value $b_{ij}$ from the intervention matrix $B$.
	 */
	default double unscaledFlowOf(int flow, int product) {
		double[] column = unscaledFlowsOf(product);
		return column[flow];
	}

	default double[] directFlowsOf(int product) {
		var flowIdx = flowIndex();
		if (flowIdx == null)
			return new double[0];
		var flows = unscaledFlowsOf(product);
		var s = scalingVector();
		if (empty(flows) || empty(s))
			return new double[0];
		var factor = s[product];
		scale(flows, factor);
		return flows;
	}

	/**
	 * Get the the direct result of the given flow and product related to the
	 * final demand of the system. This is basically the element $g_{ij}$
	 * of the column-wise scaled intervention matrix $B$:
	 * <p>
	 * $$ G = B \text{diag}(s) $$
	 */
	default double directFlowOf(int flow, int product) {
		return scalingFactorOf(product) * unscaledFlowOf(flow, product);
	}

	/**
	 * Returns the total flow results (direct + upstream) related to one unit of
	 * the given product $j$ in the system. This is the respective column $j$
	 * of the intensity matrix $M$:
	 * <p>
	 * $$M = B * A^{-1}$$
	 */
	double[] totalFlowsOfOne(int product);

	/**
	 * Returns the total result (direct + upstream) of the given flow related
	 * to one unit of the given product in the system.
	 */
	default double totalFlowOfOne(int flow, int product) {
		var totals = totalFlowsOfOne(product);
		return empty(totals)
				? 0
				: totals[flow];
	}

	default double[] totalFlowsOf(int product) {
		var factor = totalFactorOf(product);
		var totals = totalFlowsOfOne(product);
		for (int i = 0; i < totals.length; i++) {
			totals[i] *= factor;
		}
		return totals;
	}

	/**
	 * Returns the total flow result (direct + upstream) of the given flow
	 * and product related to the final demand of the system.
	 */
	default double totalFlowOf(int flow, int product) {
		double[] tr = totalRequirements();
		if (tr == null)
			return 0;
		double[] ofOne = totalFlowsOfOne(product);
		if (ofOne == null)
			return 0;
		double loop = loopFactorOf(product);
		return loop * tr[product] * ofOne[flow];
	}

	/**
	 * The inventory result $\mathbf{g}$ of the product system:
	 * <p>
	 * $$\mathbf{g} = \mathbf{B} \ \mathbf{s}$$
	 * <p>
	 * Where $\mathbf{B}$ is the intervention matrix and $\mathbf{s}$ the
	 * scaling vector. Note that inputs have negative values in this vector.
	 */
	double[] totalFlows();

	/**
	 * Get the impact factors $c_m$ for the given flow $m$ which is the $m$th
	 * column of the impact matrix $C \in \mathbb{R}^{k \times m}$:
	 * <p>
	 * $$
	 * c_m = C[:, m]
	 * $$
	 */
	double[] impactFactorsOf(int flow);

	/**
	 * Get the impact factor $c_{km}$ for the given indicator $m$ and flow $m$
	 * which is the respective entry in the impact matrix
	 * $C \in \mathbb{R}^{k \times m}$:
	 * <p>
	 * $$
	 * c_{km} = C[k, m]
	 * $$
	 */
	default double impactFactorOf(int indicator, int flow) {
		var factors = impactFactorsOf(flow);
		return factors == null ? 0 : factors[indicator];
	}

	default double[] flowImpactsOf(int flow) {
		var totals = totalFlows();
		var impacts = impactFactorsOf(flow);
		if (empty(totals) || empty(impacts))
			return new double[0];
		var total = totals[flow];
		for (int k = 0; k < impacts.length; k++) {
			impacts[k] *= total;
		}
		return impacts;
	}

	default double flowImpactOf(int indicator, int flow) {
		var totals = totalFlows();
		if (empty(totals))
			return 0;
		var factor = impactFactorOf(indicator, flow);
		return factor * totals[flow];
	}

	double[] directImpactsOf(int product);

	default double directImpactOf(int indicator, int product) {
		var impacts = directImpactsOf(product);
		return empty(impacts)
				? 0
				: impacts[product];
	}

	double[] totalImpactsOfOne(int product);

	default double totalImpactOfOne(int indicator, int product) {
		var impacts = totalImpactsOfOne(product);
		return empty(impacts)
				? 0
				: impacts[indicator];
	}

	default double[] totalImpactsOf(int product) {
		var impacts = totalImpactsOfOne(product);

	}

	double[] totalImpacts();

	double totalCosts();

	double totalCostsOfOne(int product);

	default boolean empty(double[] values) {
		return values == null || values.length == 0;
	}

	/**
	 * Scales the given vector $\mathbf{v}$ with the given factor $f$ in place:
	 * <p>
	 * $$
	 * \mathbf{v} := \mathbf{v} \odot f
	 * $$
	 */
	default void scale(double[] values, double factor) {
		if (empty(values))
			return;
		for (int i = 0; i < values.length; i++) {
			values[i] *= factor;
		}
	}
}
