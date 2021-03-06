package org.openlca.util;

import org.junit.Assert;
import org.junit.Test;
import org.openlca.core.TestProcess;
import org.openlca.core.Tests;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.Descriptor;

public class ParametersTest {

	private final IDatabase db = Tests.getDb();

	@Test
	public void testFindRedefOwners() {

		// create a process with a local parameter
		var process = TestProcess
				.refProduct("prod", 1, "kg")
				.param("param", 42)
				.get();
		var param = process.parameters.get(0);

		// create project and product system
		var project = new Project();
		project.variants.add(new ProjectVariant());
		db.insert(project);
		var system = new ProductSystem();
		system.parameterSets.add(new ParameterRedefSet());
		db.insert(system);

		// should not find something
		var owners = Parameters.findRedefOwners(
				param, process, db);
		Assert.assertTrue(owners.isEmpty());

		// add parameter redefs
		var redef = new ParameterRedef();
		redef.name = param.name;
		redef.contextId = process.id;
		redef.contextType = ModelType.PROCESS;
		redef.value = 24;
		project.variants.get(0)
				.parameterRedefs.add(redef.clone());
		project = db.update(project);
		system.parameterSets.get(0)
				.parameters.add(redef.clone());
		system = db.update(system);

		// should find something
		owners = Parameters.findRedefOwners(
				param, process, db);
		Assert.assertEquals(2, owners.size());
		Assert.assertTrue(
				owners.contains(Descriptor.of(system)));
		Assert.assertTrue(
				owners.contains(Descriptor.of(project)));
		db.clear();
	}
}
