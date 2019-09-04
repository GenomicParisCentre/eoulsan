package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.design.*;

import java.util.*;

import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getAllSamplesMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getExperimentSampleAllMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.CONDITION_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.REFERENCE_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.REP_TECH_GROUP_KEY;
import static java.util.Objects.requireNonNull;

/**
 * This class is made to check the design.txt file before running DESeq2.
 * @author Charlotte Berthelier
 * @since 2.4
 */
public class DESeq2Checker {

    /**
     * Check experiment design.
     * @param experiment experiment to check
     * @throws EoulsanException if the experiment design is not correct
     */
    static boolean checkExperimentDesign(Experiment experiment) throws EoulsanException {
        return checkExperimentDesign (experiment,true);
    }


    /**
     * Check experiment design.
     * @param experiment experiment to check
     * @param throwsException if true throw an exception
     * @throws EoulsanException if the experiment design is not correct
     */
    static boolean checkExperimentDesign(Experiment experiment, boolean throwsException) throws EoulsanException {

        requireNonNull(experiment, "Experiment argument cannot be null");
        final ExperimentMetadata emd = experiment.getMetadata();
        final Design design = experiment.getDesign();

        // Get the name of all keys
        List<String> esColumnNames = getExperimentSampleAllMetadataKeys(experiment);
        List<String> sColumnNames = getAllSamplesMetadataKeys(design);
        List<String> allColumnNames= new ArrayList<>(esColumnNames);
        allColumnNames.addAll(sColumnNames);

        /**
         * Check if there is an empty cell in the experiment
         * */
        for (String key : allColumnNames) {
            for (ExperimentSample es : experiment.getExperimentSamples()) {
                String value = DesignUtils.getMetadata(es, key);
                if(Strings.isNullOrEmpty(value)){
                    return error("There is an empty cell" + experiment.getName(), throwsException);
                }
            }
        }

        /**
         * Check if the comparison string is correct
         * */
        if (emd.containsComparisons()) {
            Set<String> comparisionNames=new HashSet<>();

            // Check if the comparison structure is correct
            for (String c : emd.getComparisons().split(";")) {
                String[] splitC = Splitter.on(':').omitEmptyStrings().trimResults().splitToList(c).
                        toArray(new String[0]);

                // Check if there is not more than one value per comparison
                if (c.split(":").length != 2) {
                    return error("Error in " + experiment.getName()
                            + " experiment, comparison cannot have more than 1 value: " + c, throwsException);
                }

                // Get the name of each comparison
                for (String item : splitC){
                    // Error if comparison string equals to "vs" or "XXX_vs_" or "_vs_XXX"
                    if (item.equals("vs") || item.startsWith("_vs") || item.endsWith("vs_")){
                        return error("Error in " + experiment.getName()
                                + " experiment, the comparison string is badly written : " + c, throwsException);
                    }
                    else if (!item.contains("_vs_")){
                        comparisionNames.add(item);
                    }
                }

                // Get each condition in the comparison string
                Set<String> conditionsInComparisonString=new HashSet<>(Arrays.asList(splitC[1].
                        split("(%)|(_vs_)")));

                // Get every sample value for each key, and get every possible condition by merging the strings
                Set<String> possibleConditions=new HashSet<>();
                for (String key : allColumnNames) {
                    for (ExperimentSample es : experiment.getExperimentSamples()) {
                        String value = DesignUtils.getMetadata(es, key);
                        possibleConditions.add(key+value);
                    }
                }
                // Check if each conditions in the comparison string exist in the Condition column
                for (String condi : conditionsInComparisonString){
                    Boolean exist = false;
                    for (String s: possibleConditions){
                        if (s.equals(condi)){
                            exist = true;
                        }
                    }
                    if (!exist){
                        return error("Error in " + experiment.getName()
                                + " experiment, one comparison does not exist: " + c, throwsException);
                    }
                }
            }

            // Check if the name of each comparison is unique
            Set<String> set=new HashSet<>();
            Set<String> duplicateElements=new HashSet<>();
            for (String element : comparisionNames) {
                if(!set.add(element)){
                    duplicateElements.add(element);
                }
            }
            if (!duplicateElements.isEmpty()){
                return error("Error in " + experiment.getName() + " experiment, there is one or more " +
                        "duplicates in the comparison string names", throwsException);
            }
        }

        /**
         * Check if there is no numeric character at the begin of a row in the column Condition
         * for a complex design model
         * */
        if (esColumnNames.contains(CONDITION_KEY) ||
                sColumnNames.contains(CONDITION_KEY)) {
            for (ExperimentSample es : experiment.getExperimentSamples()) {
                String s = DesignUtils.getMetadata(es, CONDITION_KEY);
                // Error if a condition column contains an invalid numeric character as first character
                if ( !s.isEmpty() && Character.isDigit(s.charAt(0)) && emd.getComparisons() != null){
                    return error("One or more Condition rows start with a numeric character : "
                            + experiment.getName(), throwsException);
                }
            }
        }

        /* Check if there is no "-" in the column Condition when the contrast mode is activate */
        if (esColumnNames.contains(CONDITION_KEY) ||
                sColumnNames.contains(CONDITION_KEY)) {
            for (ExperimentSample es : experiment.getExperimentSamples()) {
                String s = DesignUtils.getMetadata(es, CONDITION_KEY);
                if (s.indexOf('-') != -1 && emd.isContrast()){
                    return error("There is a - character in the column Condition : "
                            + experiment.getName(), throwsException);
                }
            }
        }

        /**
         * Verify consistency between the values in the columns Reference and Condition for non complex mode
         * */
        if (!emd.containsComparisons()) {
            // If Exp.exp1.Condition and Exp.exp1.Reference exist
            if (esColumnNames.contains(REFERENCE_KEY)
                    && esColumnNames.contains(CONDITION_KEY)){
                Map<String, String> lhm = new HashMap<>();

                // Get all samples values of the Condition and Reference columns
                for (ExperimentSample es : experiment.getExperimentSamples()) {
                    String condition = DesignUtils.getMetadata(es, CONDITION_KEY);
                    String reference = DesignUtils.getMetadata(es, REFERENCE_KEY);

                    // Check if condition and reference are not null or empty
                    if (condition.isEmpty() || reference.isEmpty()){
                        return error("There is an empty condition or reference " +
                                experiment.getName(), throwsException);
                    }

                    // If one condition is associated with more than one reference, error
                    List<String> possibleConditionsReferences = new ArrayList<>();
                    possibleConditionsReferences.add(condition+reference);
                    for (Map.Entry<String, String> e : lhm.entrySet()) {
                        String key = e.getKey();
                        String value = e.getValue();
                        if (key.equals(condition) && !value.equals(reference)){
                            return error("There is an inconsistency between the conditions " +
                                    "and the references : " + experiment.getName(), throwsException);
                        }
                    }
                    lhm.put(condition,reference);
                }
            }
        }

        /**
         * Check if there is no combination column-name that is equal to another column-name or to a column name
         * */
        // Multimap containing every key and all sample values for each
        Multimap<String, String> mapPossibleCombination = ArrayListMultimap.create();
        for (String key : allColumnNames) {
            for (ExperimentSample es : experiment.getExperimentSamples()) {
                String value = DesignUtils.getMetadata(es, key);
                mapPossibleCombination.put(key,value);
            }
        }

        // Test the coherence between the key and the values names
        for (String key : mapPossibleCombination.keySet()){
            for (String value : mapPossibleCombination.values()){
                for (String keybis : mapPossibleCombination.keySet()){
                    // keyValue contains all possible key+value combinations
                    String keyValue = keybis+value;
                    // Error if: key = value or key+value = key
                    if (key.equals(value) || keyValue.equals(key)){
                        return error("There is an incoherence between the key and the values names" +
                                experiment.getName(), throwsException);
                    }
                }
            }
        }


        /**
         * Check if the column Condition is missing for the experiment
         * */
        if (!esColumnNames.contains(CONDITION_KEY)
                && !sColumnNames.contains(CONDITION_KEY)) {
            return error("Condition column missing for experiment: "
                    + experiment.getName(), throwsException);
        }

        /* Check if the column RepTechGroup is missing for the experiment */
        if (!esColumnNames.contains(REP_TECH_GROUP_KEY)
                && !sColumnNames.contains(REP_TECH_GROUP_KEY)) {
            return error("RepTechGroup column missing for experiment: "
                    + experiment.getName(), throwsException);
        }

        return true;
    }

        private static boolean error (String message, boolean throwsException) throws EoulsanException {

        if (throwsException){
            throw new EoulsanException(message);
        }

        return false;
    }

}
