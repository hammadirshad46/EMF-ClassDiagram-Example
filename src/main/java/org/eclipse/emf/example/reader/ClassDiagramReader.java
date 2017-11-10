package org.eclipse.emf.example.reader;


import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.example.models._class.*;
import org.eclipse.emf.example.models._enum.EnumStructure;
import org.eclipse.emf.example.models._package.PackageStructure;
import org.eclipse.emf.example.util.Keywords;
import org.eclipse.uml2.uml.*;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.internal.impl.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassDiagramReader implements Serializable {
    private static final long serialVersionUID = 1L;

    public static ClassDiagram getRefModelDetails(Package _package) {

        ClassDiagram classDiagram = new ClassDiagram();

        PackageStructure packageStructure;
        if (_package != null) {
            EList<PackageableElement> packageableElements = _package.getPackagedElements();
            String packageName = _package.getName() != null ? _package.getName() : "";
            packageStructure = readPackage(packageableElements, packageName);
        } else {
            System.err.println("Package is null");
            return null;
        }

        Map<String, ClassStructure> classes = classStructures(packageStructure);
        for (ClassStructure cs : classes.values()) {
            List<ClassStructure> superClasses = new ArrayList<>();
            for (ClassStructure superClass : cs.getSuperClasses()) {
                superClasses.add(classes.get(superClass.getName()));
            }
            cs.setSuperClasses(superClasses);


        }

        Map<String, ClassInstance> instances = classInstances(packageStructure);

        for (ClassInstance classInstance : instances.values()) {
            for (ClassStructure classStructure : classInstance.getClasses()) {
                classes.get(classStructure.getName()).getInstances().add(classInstance);
            }
        }

        classDiagram.getEnumerations().addAll(enumStructure(packageStructure).values());
        classDiagram.getClasses().addAll(classes.values());
        classDiagram.getInstances().addAll(instances.values());

        return classDiagram;
    }

    private static Map<String, ClassInstance> classInstances(PackageStructure packageStructure) {
        Map<String, ClassInstance> instances = new HashMap<>();

        for (ClassInstance classInstance : packageStructure.getInstances()) {
            instances.put(classInstance.getName(), classInstance);
        }

        for (PackageStructure ps : packageStructure.getPackages()) {
            instances.putAll(classInstances(ps));
        }
        return instances;
    }

    private static Map<String, ClassStructure> classStructures(PackageStructure packageStructure) {
        Map<String, ClassStructure> classes = new HashMap<>();

        for (ClassStructure classStructure : packageStructure.getClasses()) {
            classes.put(classStructure.getName(), classStructure);
        }

        for (PackageStructure ps : packageStructure.getPackages()) {
            classes.putAll(classStructures(ps));
        }
        return classes;
    }


    private static Map<String, EnumStructure> enumStructure(PackageStructure packageStructure) {
        Map<String, EnumStructure> enums = new HashMap<>();

        for (EnumStructure classStructure : packageStructure.getEnums()) {
            enums.put(classStructure.getName(), classStructure);
        }

        for (PackageStructure ps : packageStructure.getPackages()) {
            enums.putAll(enumStructure(ps));
        }
        return enums;
    }

    private static PackageStructure readPackage(EList<PackageableElement> packageableElements, String packageName) {

        PackageStructure packageStructure = new PackageStructure();
        packageStructure.setName(packageName);

        for (PackageableElement element : packageableElements) {

            if (element.eClass() == UMLPackage.Literals.CLASS) {
                ClassStructure classStructure = readClass(element, packageName);
                packageStructure.getClasses().add(classStructure);
            } else if (element.eClass() == UMLPackage.Literals.ENUMERATION) {
                EnumStructure enumStructure = readEnumeration(element, packageName);
                packageStructure.getEnums().add(enumStructure);
            } else if (element.eClass() == UMLPackage.eINSTANCE.getInstanceSpecification()) {
                ClassInstance classInstance = readInstance(element, packageName);
                packageStructure.getInstances().add(classInstance);
            } else if (element.eClass() == UMLPackage.Literals.PACKAGE) {
                Package _package = (Package) element;
                String newPackageName;

                if (packageName.equals("")) {
                    newPackageName = _package.getName() != null
                            ? _package.getName()
                            : packageName;
                } else {
                    newPackageName = _package.getName() != null
                            ? packageName + "." + _package.getName()
                            : packageName;
                }
                PackageStructure nustedPackageStructure = readPackage(_package.getPackagedElements(), newPackageName);
                packageStructure.getPackages().add(nustedPackageStructure);
            }
        }
        return packageStructure;
    }

    private static ClassInstance readInstance(PackageableElement element, String packageName) {
        ClassInstance classInstance = new ClassInstance();
        InstanceSpecification instance = (InstanceSpecification) element;
        if (instance.getName() != null && !instance.getName().isEmpty()) {

            classInstance.setName(instance.getName());
            classInstance.set_package(packageName);

            for (Slot slot : instance.getSlots()) {

                StructuralFeature feature = slot.getDefiningFeature();

                InstanceAttribute attribute = new InstanceAttribute();
                attribute.setName(feature.getName());
                attribute.setType(feature.getType().getName());

                List<Object> values = new ArrayList<>();
                for (ValueSpecification valueSpecification : slot.getValues()) {

                    if (valueSpecification instanceof InstanceValue) {

                        attribute.setClass(true);
                        InstanceValue instanceValue = (InstanceValue) valueSpecification;

                        InstanceSpecification valueInstanceSpecification = instanceValue.getInstance();

                        if (valueInstanceSpecification != null) {
                            values.add(valueInstanceSpecification.getName());
                        }


                    } else if (valueSpecification instanceof LiteralSpecification) {

                        LiteralSpecification literalSpecification = (LiteralSpecification) valueSpecification;


                        if (literalSpecification instanceof LiteralString) {
                            LiteralString literal = (LiteralString) literalSpecification;
                            values.add(literal.getValue());

                        } else if (literalSpecification instanceof LiteralInteger) {
                            LiteralInteger literal = (LiteralInteger) literalSpecification;
                            values.add(literal.getValue());


                        } else if (literalSpecification instanceof LiteralBoolean) {
                            LiteralBoolean literal = (LiteralBoolean) literalSpecification;
                            values.add(literal.isValue());

                        } else if (literalSpecification instanceof LiteralReal) {
                            LiteralReal literal = (LiteralReal) literalSpecification;
                            values.add(literal.getValue());

                        } else if (literalSpecification instanceof LiteralUnlimitedNatural) {
                            LiteralUnlimitedNatural literal = (LiteralUnlimitedNatural) literalSpecification;
                            values.add(literal.getValue());

                        }

                    }


                }

                attribute.setValues(values.toArray());
                classInstance.addAttribute(attribute);


            }


            for (Classifier classifier : instance.getClassifiers()) {
                if (instance.getName() != null && classifier.getName() != null) {
                    ClassStructure classStructure = new ClassStructure();
                    classStructure.setName(classifier.getName());
                    classStructure.setPackage(classifier.getPackage().getName());
                    classInstance.getClasses().add(classStructure);
                }
            }
        }

        return classInstance;

    }

    public static EnumStructure readEnumeration(PackageableElement element, String packageName) {

        EnumStructure structure = new EnumStructure();
        Enumeration enumeration = (Enumeration) element;
        structure.setName(enumeration.getName());
        structure.setPackage(packageName);
        for (EnumerationLiteral literal : enumeration.getOwnedLiterals()) {
            structure.addLiteral(literal.getName());
        }

        return structure;
    }

    private static ClassStructure readClass(Element element, String packageName) {
        ClassStructure classStructure = new ClassStructure();
        Class _class = (Class) element;
        List<String> rules = new ArrayList<>();
        //System.out.println(_class.getName());


        for (Constraint constraint : _class.getOwnedRules()) {
            if (constraint.getSpecification() instanceof OpaqueExpressionImpl) {
                OpaqueExpressionImpl expressionImpl = (OpaqueExpressionImpl) constraint.getSpecification();
                rules.addAll(expressionImpl.getBodies());
            }

        }


        for (Class superClass : _class.getSuperClasses()) {

            ClassStructure superClassStructure = new ClassStructure();
            classStructure.setName(superClass.getName());
            classStructure.setPackage(superClass.getPackage().getName());
            classStructure.getSuperClasses().add(superClassStructure);

        }


        //System.out.println("\n  -------- \n");

        classStructure.setPackage(packageName);
        classStructure.setVisibility(_class.getVisibility().toString());
        classStructure.setRules(rules);
        classStructure.setAbstract(_class.isAbstract());
        classStructure.setFinal(_class.isLeaf());
        classStructure.setName(_class.getName());
        classStructure.setAttributes(readAttribute(_class));
        classStructure.setOperations(readClassOperations(_class));
        classStructure.setRelationships(readClassRelations(_class));


        for (NamedElement inheritedElement : _class.getInheritedMembers()) {

            if (inheritedElement instanceof Property) {
                Property property = (Property) inheritedElement;
                ClassAttribute attribute = readAttribute(property);
                if (attribute != null && attribute.getName() != null) {
                    classStructure.addAttribute(attribute);
                }
            } else if (inheritedElement instanceof Operation) {
                Operation operation = (Operation) inheritedElement;
                ClassOperation classOperation = readClassOperation(operation);
                if (classOperation != null) {
                    classStructure.addOperation(classOperation);
                }
            }

        }


        return classStructure;
    }

    private static ArrayList<ClassRelation> readClassRelations(Class _class) {
        ArrayList<ClassRelation> list = new ArrayList<>();
        EList<Relationship> classRelationships = _class.getRelationships();
        for (Relationship relationship : classRelationships) {
            if (relationship.eClass() == UMLPackage.Literals.ASSOCIATION) {
                list.add(readAssociation(relationship));
            } else if (relationship.eClass() == UMLPackage.Literals.GENERALIZATION) {
                list.add(readGeneralization(relationship));
            }

        }
        return list;
    }


    private static ArrayList<ClassOperation> readClassOperations(Class _class) {
        ArrayList<ClassOperation> operations = new ArrayList<>();
        List<Operation> ownedOperations = _class.getOwnedOperations();
        if (!ownedOperations.isEmpty()) {
            for (Operation operation : ownedOperations) {

                ClassOperation classOperation = readClassOperation(operation);
                if (classOperation != null) {
                    operations.add(classOperation);
                }

            }
        }
        return operations;
    }

    private static ClassOperation readClassOperation(Operation operation) {

        ClassOperation classOperation = new ClassOperation();
        classOperation.setName(operation.getName());
        classOperation.setVisibility(operation.getVisibility().toString());
        classOperation.setReturnType(new OperationReturn());

        EList<Parameter> parameters = operation.getOwnedParameters();
        if (!parameters.isEmpty()) {
            for (Parameter parameter : parameters) {

                boolean returnType = false;


                ParameterDirectionKind direction = parameter.getDirection();
                if (direction != null && direction.getValue() == 3) {
                    returnType = true;
                }

                OperationParameter operationParameter = new OperationParameter();

                if (parameter
                        .getType() instanceof PrimitiveTypeImpl) {
                    PrimitiveTypeImpl primitiveType = (PrimitiveTypeImpl) (parameter.getType());
                    if (returnType) {
                        if (primitiveType.getName() == null || primitiveType.getName().equals("")) {
                            OperationReturn methodReturn = new OperationReturn();
                            methodReturn.setType(primitiveType.eProxyURI().fragment());
                            if (parameter.getUpper() == -1) {
                                methodReturn.setCollection(true);
                            }
                            classOperation.setReturnType(methodReturn);
                        } else {
                            OperationReturn methodReturn = new OperationReturn();
                            methodReturn.setType(primitiveType.getName());
                            if (parameter.getUpper() == -1) {
                                methodReturn.setCollection(true);
                            }
                            classOperation.setReturnType(methodReturn);

                        }

                    } else {
                        operationParameter.setName(parameter.getName());

                        if (primitiveType.getName() == null || primitiveType.getName().equals("")) {
                            operationParameter.setType(primitiveType.eProxyURI().fragment());
                            if (parameter.getUpper() == -1) {
                                operationParameter.setCollection(true);
                            }
                        } else {
                            if (parameter.getUpper() == -1) {
                                operationParameter.setCollection(true);
                            }
                            operationParameter.setType(primitiveType.getName());
                        }
                        classOperation.getParameters().add(operationParameter);
                    }
                } else if (parameter
                        .getType() instanceof Enumeration) {

                    Enumeration enumeration = (Enumeration) (parameter.getType());

                    if (returnType) {
                        OperationReturn methodReturn = new OperationReturn();
                        methodReturn.setType(enumeration.getName());
                        if (parameter.getUpper() == -1) {
                            methodReturn.setCollection(true);
                        }
                        classOperation.setReturnType(methodReturn);

                    } else {

                        operationParameter.setName(parameter.getName());
                        operationParameter.setVisibility(parameter.getVisibility().toString());
                        operationParameter.setType(enumeration.getName());
                        if (parameter.getUpper() == -1) {
                            operationParameter.setCollection(true);
                        }
                        classOperation.getParameters().add(operationParameter);
                    }

                } else if (parameter
                        .getType() instanceof Class) {
                    Class _class = (Class) (parameter.getType());
                    if (returnType) {
                        OperationReturn methodReturn = new OperationReturn();
                        methodReturn.setType(_class.getName());
                        methodReturn.setClass(true);
                        if (parameter.getUpper() == -1) {
                            methodReturn.setCollection(true);
                        }
                        classOperation.setReturnType(methodReturn);

                    } else {

                        operationParameter.setName(parameter.getName());
                        operationParameter.setVisibility(parameter.getVisibility().toString());
                        operationParameter.setType(_class.getName());
                        operationParameter.setClass(true);

                        if (parameter.getUpper() == -1) {
                            operationParameter.setCollection(true);
                        }
                        classOperation.getParameters().add(operationParameter);
                    }
                } else if (parameter
                        .getType() instanceof InterfaceImpl) {

                    InterfaceImpl prim = (InterfaceImpl) (parameter.getType());
                    URI proxy = prim.eProxyURI();
                    String proxyFragment = proxy.fragment();
                    String arrtibuteType = attributeInterface(proxyFragment);
                    operationParameter.setType(arrtibuteType);

                    if (returnType) {
                        OperationReturn methodReturn = new OperationReturn();
                        methodReturn.setType(arrtibuteType);
                        if (operationParameter.isCollection()) {
                            methodReturn.setCollection(true);
                        }
                        classOperation.setReturnType(methodReturn);

                    } else {
                        operationParameter.setType(arrtibuteType);
                        operationParameter.setName(parameter.getName());
                        operationParameter.setVisibility(parameter.getVisibility().toString());
                        classOperation.getParameters().add(operationParameter);
                    }
                }

            }
        }


        return classOperation;
    }


    private static ArrayList<ClassAttribute> readAttribute(Class _class) {
        ArrayList<ClassAttribute> attributes = new ArrayList<>();
        EList<Property> ownedAttributes = _class.getOwnedAttributes();
        if (!ownedAttributes.isEmpty()) {
            for (Property property : ownedAttributes) {
                ClassAttribute attribute = readAttribute(property);
                if (attribute != null && attribute.getName() != null) {
                    attributes.add(attribute);
                }
            }
        }
        return attributes;
    }

    private static ClassAttribute readAttribute(Property property) {

        ClassAttribute attribute = new ClassAttribute();
        if (property.getType() instanceof PrimitiveTypeImpl) {
            attribute.setName(property.getName());
            attribute.setVisibility(property.getVisibility().toString());
            PrimitiveTypeImpl prim = (PrimitiveTypeImpl) (property.getType());
            if (prim.getName() == null || prim.getName().equals("")) {
                attribute.setType(prim.eProxyURI().fragment());
            } else {
                attribute.setType(prim.getName());
            }

            if (property.getUpper() == -1) {
                attribute.setCollection(true);
            }

        } else if (property
                .getType() instanceof EnumerationImpl) {

            EnumerationImpl impl = (EnumerationImpl) (property.getType());
            attribute.setName(property.getName());
            attribute.setVisibility(property.getVisibility().toString());
            attribute.setType(impl.getName());
            attribute.setEnum(true);

            if (property.getUpper() == -1) {
                attribute.setCollection(true);
            }

        } else if (property.getType() instanceof ClassImpl) {

            ClassImpl impl = (ClassImpl) (property.getType());
            attribute.setName(property.getName());
            attribute.setVisibility(property.getVisibility().toString());
            attribute.setType(impl.getName());
            attribute.setClass(true);

            if (property.getUpper() == -1) {
                attribute.setCollection(true);
            }

        } else if (property
                .getType() instanceof InterfaceImpl) {

            InterfaceImpl prim = (InterfaceImpl) (property.getType());
            URI proxy = prim.eProxyURI();
            String proxyFragment = proxy.fragment();
            attribute.setCollection(attributeInterfaceCollection(proxyFragment));
            String arrtibuteType = attributeInterface(proxyFragment);
            attribute.setType(arrtibuteType);
            attribute.setName(property.getName());
            attribute.setVisibility(property.getVisibility().toString());

        }
        return attribute;
    }


    private static ClassRelation readGeneralization(Element element) {
        Generalization generalization = (Generalization) element;
        ClassRelation relation = new ClassRelation();
        relation.setType(Keywords.Generalization);
        boolean first = true;
        for (Element elements : generalization.getRelatedElements()) {
            if (elements instanceof Class) {
                ClassImpl relationClass = (ClassImpl) elements;
                if (first) {
                    first = false;
                    relation.setClass_1(relationClass.getName());
                } else {
                    relation.setClass_2(relationClass.getName());
                }

            }
        }
        return relation;
    }


    private static  ClassRelation readAssociation(Element element) {
        Association association = (Association) element;

        ClassRelation relation = new ClassRelation();
        relation.setType(Keywords.Association);
        boolean first = true;
        for (Property end : association.getMemberEnds()) {
            if (end.getType() instanceof Class) {
                if (first) {
                    first = false;
                    relation.setVisibility(end.getVisibility().toString());

                    relation.setClass_1(end.getType().getName());
                    if (end.getName() != null && !end.getName().isEmpty()) {
                        relation.setRole_Name_1(end.getName());
                    } else {
                        relation.setRole_Name_1("");
                    }
                    relation.setNavigable_1(end.isNavigable());


                    LiteralUnlimitedNatural upperValue =
                            (LiteralUnlimitedNatural) end.getUpperValue();
                    if (upperValue != null) {
                        relation.setMultipcity_Uper_1(upperValue.getValue());
                    }

                    if (end instanceof LiteralUnlimitedNatural) {

                        LiteralUnlimitedNatural lowerValue =
                                (LiteralUnlimitedNatural) end.getLowerValue();
                        relation.setMultipcity_Lower_1(lowerValue.getValue());

                    }


                } else {

                    relation.setClass_2(end.getType().getName());
                    if (end.getName() != null && !end.getName().isEmpty()) {
                        relation.setRole_Name_2(end.getName());
                    } else {
                        relation.setRole_Name_2("");
                    }
                    relation.setNavigable_2(end.isNavigable());

                    LiteralUnlimitedNatural upperValue =
                            (LiteralUnlimitedNatural) end.getUpperValue();
                    if (upperValue != null) {
                        relation.setMultipcity_Uper_2(upperValue.getValue());
                    }

                    if (end instanceof LiteralUnlimitedNatural) {

                        LiteralUnlimitedNatural lowerValue =
                                (LiteralUnlimitedNatural) end.getLowerValue();
                        relation.setMultipcity_Lower_2(lowerValue.getValue());
                    }

                    return relation;
                }

            }

        }


        return relation;
    }


    private static boolean attributeInterfaceCollection(String proxyFragment) {
        boolean isCollection = false;
        if (proxyFragment != null && !proxyFragment.isEmpty()) {
            if (proxyFragment.contains("java.util.List") || proxyFragment.contains("java.util.ArrayList")) {
                isCollection = true;
            }
        }

        return isCollection;
    }

    private static String attributeInterface(String proxyFragment) {
        String arrtibuteType = "";
        boolean isCollection = false;
        if (proxyFragment != null && !proxyFragment.isEmpty()) {

            String collectionType = "";
            if (proxyFragment.contains("java.util.List")) {
                isCollection = true;
                collectionType = "java.util.List";
            } else if (proxyFragment.contains("java.util.ArrayList")) {
                isCollection = true;
                collectionType = "java.util.ArrayList";
            }


            if (isCollection) {
                int startIndex = proxyFragment.indexOf(collectionType + "[project^id=");
                startIndex += (collectionType + "[project^id=").length();
                String temp =
                        proxyFragment.substring(startIndex, proxyFragment.indexOf("]$uml.Interface"));
                arrtibuteType = collectionType + "<" + temp + ">";

            } else {

                int startIndex = proxyFragment.indexOf(collectionType + "[project^id=");
                startIndex += ("[project^id=").length();

                arrtibuteType =
                        proxyFragment.substring(startIndex, proxyFragment.indexOf("]$uml.Interface"));
            }


        }

        return arrtibuteType;
    }

}