package org.swrlapi.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.Node;

import java.util.*;

public class OntologyUtil {
    static public OntologyHelper initHelper(List<String> texts) {
        OntologyHelper helper = new OntologyHelper(new Expression(texts));
        return helper;
    }

    static public OntologyHelper initHelper(Expression expression) {
        OntologyHelper helper = new OntologyHelper(expression);
        return helper;
    }

    static public Expression getExpressionFromJson(String jsonExpression) {
        return new Expression(
                new Gson().fromJson(
                        jsonExpression,
                        new TypeToken<List<String>>() {}.getType()));
    }

    class Relation {
        int indexFrom;
        List<Integer> indexesTo;
    }

    static public HashMap<Integer, Set<Integer>> getObjectPropertyRelationsByIndexFromJson(String jsonRelations, int maxIndex) {
        HashMap<Integer, Set<Integer>> result = new HashMap<>();
        for (int i = 1; i <= maxIndex; i++) {
            result.put(i, new HashSet<>());
        }

        Relation[] relations = new Gson().fromJson(
                jsonRelations,
                Relation[].class);

        for (Relation relation : relations) {
            for (Integer indexTo : relation.indexesTo) {
                result.get(relation.indexFrom).add(indexTo);
            }
        }

        return result;
    }

    static public HashMap<Integer, Set<Integer>> getObjectPropertyRelationsByIndex(OntologyHelper helper, String objectProperty) {
        HashMap<Integer, Set<Integer>> relations = new HashMap<>();

        for (OWLNamedIndividual ind : helper.getSortedIndividuals(helper.getIndividuals())) {

            OWLDataProperty dpIndex = helper.getDataProperty("index");
            OWLObjectProperty opProperty = helper.getObjectProperty(objectProperty);

            int index = Integer.parseInt(helper.getDataValue(ind, dpIndex));

            Set<Integer> indIndexes = new HashSet<>();

            for (Node<OWLNamedIndividual> sameOpInd : helper.getReasoner().getObjectPropertyValues(ind, opProperty)) {
                OWLNamedIndividual opInd = sameOpInd.getRepresentativeElement();
                int opIndex = Integer.parseInt(helper.getDataValue(opInd, dpIndex));
                indIndexes.add(opIndex);
            }

            relations.put(index, indIndexes);
        }

        return relations;
    }

    /**
     * Not optimal. Check OWL API to find quicker method
     */
    static public boolean checkObjectPropertyExist(OntologyHelper helper, OWLNamedIndividual a, OWLNamedIndividual b, OWLObjectProperty property) {
        for (Node<OWLNamedIndividual> sameOpInd : helper.getReasoner().getObjectPropertyValues(a, property)) {
            OWLNamedIndividual opInd = sameOpInd.getRepresentativeElement();
            if (opInd.equals(b)) {
                return true;
            }
        }
        return false;
    }

    static public HashMap<Integer, String> getDataProperties(OntologyHelper helper, String dataProperty) {
        HashMap<Integer, String> properties = new HashMap<>();

        for (OWLNamedIndividual ind : helper.getSortedIndividuals(helper.getIndividuals())) {

            OWLDataProperty dpIndex = helper.getDataProperty("index");

            int index = Integer.parseInt(helper.getDataValue(ind, dpIndex));
            properties.put(index, helper.getDataValue(ind, helper.getDataProperty(dataProperty)));
        }

        return properties;
    }

    static public Set<StudentError> GetErrors(OntologyHelper helper, boolean debug) {
        Set<StudentError> resultErrors = new HashSet<>();
        if (debug) {
            helper.dump(true);
        }
        FillErrors(
                resultErrors,
                getObjectPropertyRelationsByIndex(helper, "student_error_more_priority_left"),
                StudentErrorType.HIGH_PRIORITY_TO_LEFT);
        FillErrors(
                resultErrors,
                getObjectPropertyRelationsByIndex(helper, "student_error_more_priority_right"),
                StudentErrorType.HIGH_PRIORITY_TO_RIGHT);
        FillErrors(
                resultErrors,
                getObjectPropertyRelationsByIndex(helper, "student_error_left_assoc"),
                StudentErrorType.LEFT_ASSOC_TO_LEFT);
        FillErrors(
                resultErrors,
                getObjectPropertyRelationsByIndex(helper, "student_error_right_assoc"),
                StudentErrorType.RIGHT_ASSOC_TO_RIGHT);
        FillErrors(
                resultErrors,
                getObjectPropertyRelationsByIndex(helper, "student_error_in_complex"),
                StudentErrorType.IN_COMPLEX);
        FillErrors(
                resultErrors,
                getObjectPropertyRelationsByIndex(helper, "student_error_strict_operands_order"),
                StudentErrorType.STRICT_OPERANDS_ORDER);
        return resultErrors;
    }

    static void FillErrors(Set<StudentError> resultErrors, HashMap<Integer, Set<Integer>> errors, StudentErrorType type) {
        errors.forEach((error,reasons) -> {
            for (Integer reason : reasons) {
                resultErrors.add(new StudentError(error, reason, type));
            }
        });
    }

    static public List<org.swrlapi.example.Relation> GetRelations(OntologyHelper helper) {
        List<org.swrlapi.example.Relation> relations = new ArrayList<>();
        AddToRelations(relations, getObjectPropertyRelationsByIndex(helper, "before_direct"), "before_direct");
        AddToRelations(relations, getObjectPropertyRelationsByIndex(helper, "before_by_third_operator"), "before_by_third_operator");
        AddToRelations(relations, getObjectPropertyRelationsByIndex(helper, "before_third_operator"), "before_third_operator");
        AddToRelations(relations, getObjectPropertyRelationsByIndex(helper, "before_as_operand"), "before_as_operand");
        AddToRelations(relations, getObjectPropertyRelationsByIndex(helper, "has_operand"), "has_operand");
        return relations;
    }

    static void AddToRelations(List<org.swrlapi.example.Relation> relations, HashMap<Integer, Set<Integer>> props, String type) {
        for (Map.Entry<Integer, Set<Integer>> kv : props.entrySet()) {
            for (Integer to : kv.getValue()) {
                relations.add(new org.swrlapi.example.Relation(kv.getKey(), to, type));
            }
        }
    }

    static public Set<Integer> getOperandPositions(OntologyHelper helper) {
        Set<Integer> result = new HashSet<>();

        HashMap<Integer, String> props = getDataProperties(helper, "is_operand");
        for (Map.Entry<Integer, String> kv : props.entrySet()) {
            if (!kv.getValue().isEmpty()) {
                result.add(kv.getKey() - 1);
            }
        }
        return result;
    }

    public static String getDataProperty(OntologyHelper helper, int index, String property) {
        return helper.getDataValue(helper.getIndividual(0, index), helper.getDataProperty(property));
    }

    public static String getErrorDescription(StudentError error, OntologyHelper helper) {
        String errorText = getDataProperty(helper, error.getErrorPos(), "text");
        String reasonText = getDataProperty(helper, error.getReasonPos(), "text");

        HashMap<Integer, Set<Integer>> thirdOperators = getObjectPropertyRelationsByIndex(helper, "before_third_operator");
        Set<Integer> thirdOperatorPoss = thirdOperators.getOrDefault(error.getReasonPos(), new HashSet<>());
        Integer thirdOperatorPos = thirdOperatorPoss.isEmpty() ? -1 : thirdOperatorPoss.iterator().next();
        String thirdOperatorText = getDataProperty(helper, thirdOperatorPos, "text");

        if (error.Type == StudentErrorType.HIGH_PRIORITY_TO_LEFT || error.Type == StudentErrorType.HIGH_PRIORITY_TO_RIGHT) {
            return "Operator " + reasonText + " on pos " + error.getReasonPos()
                + " evaluated before operator " + errorText + " on pos " + error.getErrorPos()
                + " as operator with higher precedence";
        } else if (error.Type == StudentErrorType.LEFT_ASSOC_TO_LEFT) {
            return "Operator " + reasonText + " on pos " + error.getReasonPos()
                    + " evaluated before operator " + errorText + " on pos " + error.getErrorPos()
                    + " as operator with left associativity";
        } else if (error.Type == StudentErrorType.RIGHT_ASSOC_TO_RIGHT) {
            return "Operator " + reasonText + " on pos " + error.getReasonPos()
                    + " evaluated before operator " + errorText + " on pos " + error.getErrorPos()
                    + " as operator with right associativity";
        } else if (error.Type == StudentErrorType.IN_COMPLEX) {
            return "Operator " + reasonText + " on pos " + error.getReasonPos()
                    + " evaluated before operator " + errorText + " on pos " + error.getErrorPos()
                    + " as operator in complex operator " + thirdOperatorText + " on pos " + thirdOperatorPos;
        } else if (error.Type == StudentErrorType.STRICT_OPERANDS_ORDER) {
            return "Operator " + reasonText + " on pos " + error.getReasonPos()
                    + " evaluated before operator " + errorText + " on pos " + error.getErrorPos()
                    + " as first operand of operator" + thirdOperatorText + " on pos " + thirdOperatorPos
                    + " with strict evaluation order operands";
        } else {
            return "Unknown error";
        }
    }
}
