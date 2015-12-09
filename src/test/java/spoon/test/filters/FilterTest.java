package spoon.test.filters;

import org.junit.Before;
import org.junit.Test;
import spoon.Launcher;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.code.CtCFlowBreak;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.AnnotationFilter;
import spoon.reflect.visitor.filter.CompositeFilter;
import spoon.reflect.visitor.filter.FieldAccessFilter;
import spoon.reflect.visitor.filter.FilteringOperator;
import spoon.reflect.visitor.filter.NameFilter;
import spoon.reflect.visitor.filter.OverridingMethodFilter;
import spoon.reflect.visitor.filter.RegexFilter;
import spoon.reflect.visitor.filter.ReturnOrThrowFilter;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.test.TestUtils;
import spoon.test.filters.testclasses.AbstractTostada;
import spoon.test.filters.testclasses.Antojito;
import spoon.test.filters.testclasses.ITostada;
import spoon.test.filters.testclasses.SubTostada;
import spoon.test.filters.testclasses.Tacos;
import spoon.test.filters.testclasses.Tostada;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FilterTest {

	Factory factory;

	@Before
	public void setup() throws Exception {
		Launcher spoon = new Launcher();
		factory = spoon.createFactory();
		spoon.createCompiler(factory, SpoonResourceHelper.resources("./src/test/java/spoon/test/filters/Foo.java")).build();
	}

	@Test
	public void testFilters() throws Exception {
		CtClass<?> foo = factory.Package().get("spoon.test.filters").getType("Foo");
		assertEquals("Foo", foo.getSimpleName());
		List<CtExpression<?>> expressions = foo.getElements(new RegexFilter<CtExpression<?>>(".* = .*"));
		assertEquals(2, expressions.size());
	}

	@Test
	public void testReturnOrThrowFilter() throws Exception {
		CtClass<?> foo = factory.Package().get("spoon.test.filters").getType("Foo");
		assertEquals("Foo", foo.getSimpleName());
		List<CtCFlowBreak> expressions = foo.getElements(new ReturnOrThrowFilter());
		assertEquals(2, expressions.size());
	}


	@Test
	public void testFieldAccessFilter() throws Exception {
		// also specifies VariableAccessFilter since FieldAccessFilter is only a VariableAccessFilter with additional static typing
		CtClass<?> foo = factory.Package().get("spoon.test.filters").getType("Foo");
		assertEquals("Foo", foo.getSimpleName());

		List<CtNamedElement> elements = foo.getElements(new NameFilter<>("i"));
		assertEquals(1, elements.size());

		CtFieldReference<?> ref = (CtFieldReference<?>)(elements.get(0)).getReference();
		List<CtFieldAccess<?>> expressions = foo.getElements(new FieldAccessFilter(ref));
		assertEquals(2, expressions.size());
	}


	@Test
	public void testAnnotationFilter() throws Exception {
		CtClass<?> foo = factory.Package().get("spoon.test.filters").getType("Foo");
		assertEquals("Foo", foo.getSimpleName());
		List<CtElement> expressions = foo.getElements(new AnnotationFilter<>(SuppressWarnings.class));
		assertEquals(2, expressions.size());
		List<CtMethod> methods = foo.getElements(new AnnotationFilter<>(CtMethod.class, SuppressWarnings.class));
		assertEquals(1, methods.size());
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void filteredElementsAreOfTheCorrectType() throws Exception {
		Factory factory = TestUtils.build("spoon.test", "SampleClass").getFactory();
		Class<CtMethod> filterClass = CtMethod.class;
		TypeFilter<CtMethod> statementFilter = new TypeFilter<CtMethod>(filterClass);
		List<CtMethod> elements = Query.getElements(factory, statementFilter);
		for (CtMethod element : elements) {
			assertTrue(filterClass.isInstance(element));
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	public void intersectionOfTwoFilters() throws Exception {
		Factory factory = TestUtils.build("spoon.test", "SampleClass").getFactory();
		TypeFilter<CtMethod> statementFilter = new TypeFilter<CtMethod>(CtMethod.class);
		TypeFilter<CtMethodImpl> statementImplFilter = new TypeFilter<CtMethodImpl>(CtMethodImpl.class);
		CompositeFilter compositeFilter = new CompositeFilter(FilteringOperator.INTERSECTION, statementFilter, statementImplFilter);

		List<CtMethod> methodsWithInterfaceSuperclass = Query.getElements(factory, statementFilter);
		List<CtMethodImpl> methodWithConcreteClass = Query.getElements(factory, statementImplFilter);

		assertEquals(methodsWithInterfaceSuperclass.size(), methodWithConcreteClass.size());
		assertEquals(methodsWithInterfaceSuperclass, methodWithConcreteClass);

		List intersection = Query.getElements(factory, compositeFilter);

		assertEquals(methodsWithInterfaceSuperclass.size(), intersection.size());
		assertEquals(methodsWithInterfaceSuperclass, intersection);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	public void unionOfTwoFilters() throws Exception {
		Factory factory = TestUtils.build("spoon.test", "SampleClass").getFactory();
		TypeFilter<CtNewClass> newClassFilter = new TypeFilter<CtNewClass>(CtNewClass.class);
		TypeFilter<CtMethod> methodFilter = new TypeFilter<CtMethod>(CtMethod.class);
		CompositeFilter compositeFilter = new CompositeFilter(FilteringOperator.UNION, methodFilter, newClassFilter);

		List filteredWithCompositeFilter = Query.getElements(factory, compositeFilter);
		List<CtMethod> methods = Query.getElements(factory, methodFilter);
		List<CtNewClass> newClasses = Query.getElements(factory, newClassFilter);

		List<CtElement> union = new ArrayList<CtElement>();
		union.addAll(methods);
		union.addAll(newClasses);

		assertEquals(methods.size() + newClasses.size(), union.size());
		assertEquals(union.size(), filteredWithCompositeFilter.size());
		assertTrue(filteredWithCompositeFilter.containsAll(union));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	public void classCastExceptionIsNotThrown() throws Exception {
		Factory factory = TestUtils.build("spoon.test", "SampleClass").getFactory();
		NameFilter<CtVariable<?>> nameFilterA = new NameFilter<CtVariable<?>>("j");
		NameFilter<CtVariable<?>> nameFilterB = new NameFilter<CtVariable<?>>("k");
		CompositeFilter compositeFilter = new CompositeFilter(FilteringOperator.INTERSECTION, nameFilterA, nameFilterB);
		List filteredWithCompositeFilter = Query.getElements(factory, compositeFilter);
		assertTrue(filteredWithCompositeFilter.isEmpty());
	}

	@Test
	public void testOverridingMethodFromAbstractClass() throws Exception {
		// contract: When we declare an abstract method on an abstract class, we must return all overriding
		// methods in sub classes and anonymous classes.
		final Launcher launcher = new Launcher();
		launcher.addInputResource("./src/test/java/spoon/test/filters/testclasses");
		launcher.setSourceOutputDirectory("./target/trash");
		launcher.run();

		final CtClass<AbstractTostada> aClass = launcher.getFactory().Class().get(AbstractTostada.class);

		final List<CtMethod<?>> overridingMethods = Query.getElements(launcher.getFactory(), new OverridingMethodFilter(aClass.getMethodsByName("prepare").get(0).getReference()));
		assertEquals(5, overridingMethods.size());
		assertEquals("spoon.test.filters.testclasses.AbstractTostada$1", overridingMethods.get(0).getParent(CtClass.class).getQualifiedName());
		assertEquals(Antojito.class, overridingMethods.get(1).getParent(CtClass.class).getActualClass());
		assertEquals(SubTostada.class, overridingMethods.get(2).getParent(CtClass.class).getActualClass());
		assertEquals("spoon.test.filters.testclasses.Tostada$1", overridingMethods.get(3).getParent(CtClass.class).getQualifiedName());
		assertEquals(Tostada.class, overridingMethods.get(4).getParent(CtClass.class).getActualClass());
	}

	@Test
	public void testOverridingMethodFromSubClassOfAbstractClass() throws Exception {
		// contract: When we ask all overriding methods from an overriding method, we must returns all methods
		// below and not above (including the declaration).
		final Launcher launcher = new Launcher();
		launcher.addInputResource("./src/test/java/spoon/test/filters/testclasses");
		launcher.setSourceOutputDirectory("./target/trash");
		launcher.run();

		final CtClass<Tostada> aTostada = launcher.getFactory().Class().get(Tostada.class);

		final List<CtMethod<?>> overridingMethods = Query.getElements(launcher.getFactory(), new OverridingMethodFilter(aTostada.getMethodsByName("prepare").get(0).getReference()));
		assertEquals(2, overridingMethods.size());
		assertEquals(SubTostada.class, overridingMethods.get(0).getParent(CtClass.class).getActualClass());
		assertEquals("spoon.test.filters.testclasses.Tostada$1", overridingMethods.get(1).getParent(CtClass.class).getQualifiedName());

		final CtClass<SubTostada> aSubTostada = launcher.getFactory().Class().get(SubTostada.class);
		assertEquals(0, Query.getElements(launcher.getFactory(), new OverridingMethodFilter(aSubTostada.getMethodsByName("prepare").get(0).getReference())).size());
	}

	@Test
	public void testOverridingMethodFromInterface() throws Exception {
		// contract: When we declare a method in an interface, we must return all overriding
		// methods in sub classes and anonymous classes.
		final Launcher launcher = new Launcher();
		launcher.addInputResource("./src/test/java/spoon/test/filters/testclasses");
		launcher.setSourceOutputDirectory("./target/trash");
		launcher.run();

		final CtInterface<ITostada> aITostada = launcher.getFactory().Interface().get(ITostada.class);

		final List<CtMethod<?>> overridingMethods = Query.getElements(launcher.getFactory(), new OverridingMethodFilter(aITostada.getMethodsByName("make").get(0).getReference()));
		assertEquals(4, overridingMethods.size());
		assertEquals(AbstractTostada.class, overridingMethods.get(0).getParent(CtClass.class).getActualClass());
		assertEquals("spoon.test.filters.testclasses.AbstractTostada$1", overridingMethods.get(1).getParent(CtClass.class).getQualifiedName());
		assertEquals(Tacos.class, overridingMethods.get(2).getParent(CtClass.class).getActualClass());
		assertEquals(Tostada.class, overridingMethods.get(3).getParent(CtClass.class).getActualClass());
	}

	@Test
	public void testOverridingMethodFromSubClassOfInterface() throws Exception {
		// contract: When we ask all overriding methods from an overriding method, we must returns all methods
		// below and not above (including the declaration).
		final Launcher launcher = new Launcher();
		launcher.addInputResource("./src/test/java/spoon/test/filters/testclasses");
		launcher.setSourceOutputDirectory("./target/trash");
		launcher.run();

		final CtClass<AbstractTostada> anAbstractTostada = launcher.getFactory().Class().get(AbstractTostada.class);

		final List<CtMethod<?>> overridingMethods = Query.getElements(launcher.getFactory(), new OverridingMethodFilter(anAbstractTostada.getMethodsByName("make").get(0).getReference()));
		assertEquals(2, overridingMethods.size());
		assertEquals("spoon.test.filters.testclasses.AbstractTostada$1", overridingMethods.get(0).getParent(CtClass.class).getQualifiedName());
		assertEquals(Tostada.class, overridingMethods.get(1).getParent(CtClass.class).getActualClass());

		final CtClass<Tostada> aTostada = launcher.getFactory().Class().get(Tostada.class);
		assertEquals(0, Query.getElements(launcher.getFactory(), new OverridingMethodFilter(aTostada.getMethodsByName("make").get(0).getReference())).size());
	}
}
