/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.business.rules.core.util;

import org.wso2.carbon.business.rules.core.bean.RuleTemplate;
import org.wso2.carbon.business.rules.core.bean.RuleTemplateProperty;
import org.wso2.carbon.business.rules.core.bean.Template;
import org.wso2.carbon.business.rules.core.bean.TemplateGroup;
import org.wso2.carbon.business.rules.core.bean.scratch.BusinessRuleFromScratch;
import org.wso2.carbon.business.rules.core.bean.template.BusinessRuleFromTemplate;
import org.wso2.carbon.business.rules.core.exceptions.RuleTemplateScriptException;
import org.wso2.carbon.business.rules.core.exceptions.TemplateManagerHelperException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Consists of methods for additional features for the exposed Template Manager service
 */
public class TemplateManagerHelper {
    private static Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    /**
     * To avoid instantiation
     */
    private TemplateManagerHelper() {
    }

    /**
     * Converts given JSON File to a JSON object
     *
     * @param jsonFile json file
     * @return JsonObject
     * @throws TemplateManagerHelperException exceptions related to business rules
     */
    public static JsonObject fileToJson(File jsonFile) throws TemplateManagerHelperException {
        JsonObject jsonObject;
        try {
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile),
                    Charset.forName("UTF-8")));
            jsonObject = gson.fromJson(reader, JsonObject.class);
        } catch (FileNotFoundException e) {
            throw new TemplateManagerHelperException("File - " + jsonFile.getName() + " not found", e);
        }
        return jsonObject;
    }

    /**
     * Converts given JSON object to TemplateGroup object
     *
     * @param jsonObject Given JSON object
     * @return TemplateGroup object
     */
    public static TemplateGroup jsonToTemplateGroup(JsonObject jsonObject) {
        String templateGroupJsonString = jsonObject.get("templateGroup").toString();
        return gson.fromJson(templateGroupJsonString, TemplateGroup.class);
    }

    /**
     * Converts given String JSON definition to BusinessRuleFromTemplate object
     *
     * @param jsonDefinition Given String JSON definition
     * @return TemplateGroup object
     */
    public static BusinessRuleFromTemplate jsonToBusinessRuleFromTemplate(String jsonDefinition) {
        return gson.fromJson(jsonDefinition,
                BusinessRuleFromTemplate.class);
    }

    /**
     * Converts given String JSON definition to BusinessRuleFromScratch object
     *
     * @param jsonDefinition Given String JSON definition
     * @return TemplateGroup object
     */
    public static BusinessRuleFromScratch jsonToBusinessRuleFromScratch(String jsonDefinition) {
        return gson.fromJson(jsonDefinition,
                BusinessRuleFromScratch.class);
    }

    public static String businessRuleFromScratchToJson(BusinessRuleFromScratch businessRuleFromScratch) {
        return gson.toJson(businessRuleFromScratch);
    }

    public static String businessRuleFromTemplateToJson(BusinessRuleFromTemplate businessRuleFromTemplate) {
        return gson.toJson(businessRuleFromTemplate);
    }

    /**
     * Checks whether a given TemplateGroup object has valid content
     * Validation criteria :
     * - name is available
     * - uuid is available
     * - At least one ruleTemplate is available
     * - Each available RuleTemplate should be valid
     *
     * @param templateGroup template group object
     * @throws TemplateManagerHelperException template manager exceptions
     */
    public static void validateTemplateGroup(TemplateGroup templateGroup)
            throws RuleTemplateScriptException, TemplateManagerHelperException {
        try {
            if (templateGroup.getName() == null) {
                throw new TemplateManagerHelperException("Invalid TemplateGroup configuration file found. " +
                        "TemplateGroup name  is null");
            }
            if (templateGroup.getName().isEmpty()) {
                throw new TemplateManagerHelperException("Invalid TemplateGroup configuration file found. " +
                        "TemplateGroup name  cannot be empty");
            }
            if (templateGroup.getUuid() == null) {
                throw new TemplateManagerHelperException("Invalid TemplateGroup configuration file found. " +
                        "UUID is null for templateGroup " + templateGroup.getName());
            }
            if (templateGroup.getUuid().isEmpty()) {
                throw new TemplateManagerHelperException("Invalid TemplateGroup configuration file found. " +
                        " UUID cannot be null for templateGroup " + templateGroup.getName());
            }
            if (templateGroup.getRuleTemplates().size() == 0) {
                throw new TemplateManagerHelperException("Invalid TemplateGroup configuration file found. " +
                        "No ruleTemplate configurations found for templateGroup " + templateGroup.getName());
            }
            for (RuleTemplate ruleTemplate : templateGroup.getRuleTemplates()) {
                validateRuleTemplate(ruleTemplate);
            }
        } catch (NullPointerException e) {
            // Occurs when no value for a key is found
            throw new TemplateManagerHelperException("A required value can not be found in the template group " +
                    "definition", e);
        }

    }

    /**
     * Checks whether a given RuleTemplate object has valid content
     * <p>
     * Validation Criteria :
     * - name is available
     * - uuid is available
     * - instanceCount is either 'one' or 'many'
     * - type is either 'template', 'input' or 'output'
     * - Only one template available if type is 'input' or 'output'; otherwise at least one template available
     * - Validate each template
     * - Templated elements from the templates, should be specified in either properties or script
     * - Validate all properties
     *
     * @param ruleTemplate rule template object
     * @throws TemplateManagerHelperException template manager exceptions
     */
    private static void validateRuleTemplate(RuleTemplate ruleTemplate)
            throws RuleTemplateScriptException, TemplateManagerHelperException {
        if (ruleTemplate.getName() == null) {
            throw new TemplateManagerHelperException("Invalid rule template - Rule template name is null ");
        }
        if (ruleTemplate.getName().isEmpty()) {
            throw new TemplateManagerHelperException("Invalid rule template - Rule template name is empty ");
        }
        if (ruleTemplate.getUuid() == null) {
            throw new TemplateManagerHelperException("Invalid rule template - UUID is null for rule template : " +
                    ruleTemplate.getName());
        }
        if (ruleTemplate.getUuid().isEmpty()) {
            throw new TemplateManagerHelperException("Invalid rule template - UUID is empty for rule template : " +
                    ruleTemplate.getName());
        }
        if (ruleTemplate.getInstanceCount() == null) {
            throw new TemplateManagerHelperException("Invalid rule template - Instance count field is null in " +
                    "ruleTemplate : " + ruleTemplate.getName());
        }
        if (!(ruleTemplate.getInstanceCount().toLowerCase().equals(TemplateManagerConstants.INSTANCE_COUNT_ONE) ||
                ruleTemplate.getInstanceCount().toLowerCase().equals(TemplateManagerConstants
                        .INSTANCE_COUNT_MANY))) {
            throw new TemplateManagerHelperException("Invalid rule template - Instance count field should be " +
                    "either 'one' or 'many' in ruleTemplate : " + ruleTemplate.getName());
        }
        if (ruleTemplate.getType() == null) {
            throw new TemplateManagerHelperException("Invalid rule template - ruleTemplate type cannot be null" +
                    "in ruleTemplate : " + ruleTemplate.getName());
        }
        if (!(ruleTemplate.getType().toLowerCase().equals(TemplateManagerConstants.RULE_TEMPLATE_TYPE_TEMPLATE) ||
                ruleTemplate.getType().toLowerCase().equals(TemplateManagerConstants.RULE_TEMPLATE_TYPE_INPUT) ||
                ruleTemplate.getType().toLowerCase().equals(TemplateManagerConstants.RULE_TEMPLATE_TYPE_OUTPUT))) {
            throw new TemplateManagerHelperException("Invalid rule template - " +
                    "invalid rule template type for rule template " +
                    "" + ruleTemplate.getUuid());
        }
        if (ruleTemplate.getTemplates() == null) {
            throw new TemplateManagerHelperException("Invalid rule template - there should be at least one " +
                    "template in ruleTemplate :" + ruleTemplate.getName());
        }
        if (ruleTemplate.getType().toLowerCase().equals(TemplateManagerConstants.RULE_TEMPLATE_TYPE_INPUT) ||
                ruleTemplate.getType().toLowerCase().equals(TemplateManagerConstants.RULE_TEMPLATE_TYPE_OUTPUT)) {
            if (ruleTemplate.getTemplates().size() != 1) {
                throw new TemplateManagerHelperException("Invalid rule template - " +
                        "there should be exactly one template for " +
                        ruleTemplate.getType() + " type rule template - " + ruleTemplate.getUuid());
            }
        } else {
            if (ruleTemplate.getTemplates().size() == 0) {
                throw new TemplateManagerHelperException("Invalid rule template - No templates found in " +
                        ruleTemplate.getType() + " type rule template - " + ruleTemplate.getUuid());
            }
        }
        for (Template template : ruleTemplate.getTemplates()) {
            validateTemplate(template, ruleTemplate.getType());
        }
        // Validate whether all templated elements have replacements
        validatePropertyTemplatedElements(ruleTemplate);
    }

    /**
     * Checks whether all the templated elements of all the templates under the given rule template,
     * are having replacement values either in properties, or in script
     *
     * @param ruleTemplate
     * @throws TemplateManagerHelperException
     */
    private static void validatePropertyTemplatedElements(RuleTemplate ruleTemplate)
            throws RuleTemplateScriptException, TemplateManagerHelperException {
        // Get script with templated elements and replace with values given in the BusinessRule
        String scriptWithTemplatedElements = ruleTemplate.getScript();

        // To store name and default value of all the properties to replace
        HashMap<String, String> propertiesMap = new HashMap<String, String>();
        Map<String, RuleTemplateProperty> ruleTemplateProperties = ruleTemplate.getProperties();

        // Put each property's name and default value
        for (Map.Entry property : ruleTemplateProperties.entrySet()) {
            propertiesMap.put(property.getKey().toString(), ((RuleTemplateProperty) property.getValue())
                    .getDefaultValue());
        }

        String runnableScript = TemplateManagerHelper.replaceRegex(scriptWithTemplatedElements,
                TemplateManagerConstants.TEMPLATED_ELEMENT_NAME_REGEX_PATTERN, propertiesMap);

        // Run the script to get all the contained variables
        Map<String, String> scriptGeneratedVariables = TemplateManagerHelper.
                getScriptGeneratedVariables(runnableScript);

        propertiesMap.putAll(scriptGeneratedVariables);

        // Validate each template for replacement value
        for (Template template : ruleTemplate.getTemplates()) {
            try {
                validateContentWithTemplatedElements(template.getContent(), propertiesMap);
            } catch (TemplateManagerHelperException e) {
                throw new TemplateManagerHelperException("Invalid template. All the templated elements are not having " +
                        "replacements", e);
            }
        }
    }

    /**
     * Checks whether all the templated elements of the given content has a replacement, in given replacements
     *
     * @param content
     * @param replacements
     */
    private static void validateContentWithTemplatedElements(String content, Map<String, String> replacements)
            throws TemplateManagerHelperException {
        Pattern templatedElementNamePattern = Pattern.compile(
                TemplateManagerConstants.TEMPLATED_ELEMENT_NAME_REGEX_PATTERN);
        Matcher templatedElementMatcher = templatedElementNamePattern.matcher(content);
        while (templatedElementMatcher.find()) {
            // If there is no replacement available
            if (replacements.get(templatedElementMatcher.group(1)) == null) {
                throw new TemplateManagerHelperException("No replacement found for '" +
                        templatedElementMatcher.group(1) + "'");
            }
        }
    }

    /**
     * Checks whether a given Template is valid
     * <p>
     * Validation Criteria :
     * - type is available
     * - content is available
     * - type should be 'siddhiApp' ('gadget' and 'dashboard' are not considered for now)
     * - exposedStremDefinition available if ruleTemplateType is either 'input' or 'output', otherwise not available
     *
     * @param template
     * @param ruleTemplateType
     * @throws TemplateManagerHelperException
     */
    private static void validateTemplate(Template template, String ruleTemplateType) throws TemplateManagerHelperException {
        if (template.getType() == null) {
            throw new TemplateManagerHelperException("Invalid template. Template type not found");
        }
        if (!(template.getType().equals(TemplateManagerConstants.TEMPLATE_TYPE_SIDDHI_APP) || template.getType()
                .equals(TemplateManagerConstants.TEMPLATE_TYPE_DASHBOARD) || template.getType().equals
                (TemplateManagerConstants.TEMPLATE_TYPE_GADGET))) {
            throw new TemplateManagerHelperException("Invalid template type");
        }
        if (template.getContent() == null) {
            throw new TemplateManagerHelperException("Invalid template. Content not found");
        }
        if (template.getContent().isEmpty()) {
            throw new TemplateManagerHelperException("Invalid template. Content can not be empty");
        }

        // If ruleTemplate type 'input' or 'output'
        if (ruleTemplateType.equals(TemplateManagerConstants.RULE_TEMPLATE_TYPE_INPUT) ||
                ruleTemplateType.equals(TemplateManagerConstants.RULE_TEMPLATE_TYPE_OUTPUT)) {
            if (template.getExposedStreamDefinition() == null) {
                throw new TemplateManagerHelperException("Invalid template. Exposed stream definition not found for " +
                        "template within a rule template of type " + ruleTemplateType);
            }
            if (!template.getType().equals(TemplateManagerConstants.TEMPLATE_TYPE_SIDDHI_APP)) {
                throw new TemplateManagerHelperException("Invalid template. " + template.getType() +
                        " is not a valid template type for a template within a rule template" +
                        "of type " + ruleTemplateType + ". Template type must be '" +
                        TemplateManagerConstants.TEMPLATE_TYPE_SIDDHI_APP + "'");
            }
        } else {
            // If ruleTemplate type 'template'
            List<String> validTemplateTypes = new ArrayList<String>() {
                {
                    add(TemplateManagerConstants.TEMPLATE_TYPE_SIDDHI_APP);
                    add(TemplateManagerConstants.TEMPLATE_TYPE_GADGET);
                    add(TemplateManagerConstants.TEMPLATE_TYPE_DASHBOARD);
                }
            };

            if (template.getExposedStreamDefinition() != null) {
                throw new TemplateManagerHelperException("Invalid template. " +
                        "exposedStreamDefinition should not exist for " +
                        "template within a rule template of type " + ruleTemplateType);
            }
            if (!validTemplateTypes.contains(template.getType())) {
                // Only siddhiApps are there for now
                throw new TemplateManagerHelperException("Invalid template. " + template.getType() +
                        " is not a valid template type for a template within a rule template" +
                        "of type " + ruleTemplateType + ". Template type must be '" +
                        TemplateManagerConstants.TEMPLATE_TYPE_SIDDHI_APP + "'");
            }
        }
    }

    /**
     * Gives the name of the given Template, which is a SiddhiApp
     *
     * @param siddhiAppTemplate
     * @return String
     * @throws TemplateManagerHelperException
     */
    public static String getSiddhiAppName(Template siddhiAppTemplate) throws TemplateManagerHelperException {
        // Content of the SiddhiApp
        String siddhiApp = siddhiAppTemplate.getContent();
        // Regex match and find name
        Pattern siddhiAppNamePattern = Pattern.compile(TemplateManagerConstants.SIDDHI_APP_NAME_REGEX_PATTERN);
        Matcher siddhiAppNameMatcher = siddhiAppNamePattern.matcher(siddhiApp);
        if (siddhiAppNameMatcher.find()) {
            return siddhiAppNameMatcher.group(1);
        }

        throw new TemplateManagerHelperException("Invalid SiddhiApp Name Found");
    }

    /**
     * Replaces values with the given regex pattern in a given string, with provided replacement values
     *
     * @param stringWithRegex
     * @param regexPatternString
     * @param replacementValues
     * @return String
     */
    public static String replaceRegex(String stringWithRegex, String regexPatternString,
                                      Map<String, String> replacementValues) throws TemplateManagerHelperException {
        StringBuffer replacedString = new StringBuffer();

        Pattern regexPattern = Pattern.compile(regexPatternString);
        Matcher regexMatcher = regexPattern.matcher(stringWithRegex);

        // When an element with regex is is found
        while (regexMatcher.find()) {
            String elementToReplace = regexMatcher.group(1);
            String elementReplacement = replacementValues.get(elementToReplace);
            // No replacement found in the given map
            if (elementReplacement == null) {
                throw new TemplateManagerHelperException("No matching replacement found for the value - " + elementToReplace);
            }
            // Replace element with regex, with the found replacement
            regexMatcher.appendReplacement(replacedString, elementReplacement);
        }
        regexMatcher.appendTail(replacedString);

        return replacedString.toString();
    }

    /**
     * Runs the script that is given as a string, and gives all the variables specified in the script
     *
     * @param script
     * @return Map of Strings
     * @throws TemplateManagerHelperException
     */
    public static Map<String, String> getScriptGeneratedVariables(String script) throws RuleTemplateScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        ScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        try {
            // Run script
            engine.eval(script);
            Map<String, Object> returnedScriptContextBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

            // Store binding variable values returned as objects, as strings
            Map<String, String> variableValues = new HashMap<>();
            for (Map.Entry variable : returnedScriptContextBindings.entrySet()) {
                if (variable.getValue() == null) {
                    variableValues.put(variable.getKey().toString(), null);
                } else {
                    variableValues.put(variable.getKey().toString(), variable.getValue().toString());
                }
            }
            return variableValues;
        } catch (ScriptException e) {
            throw new RuleTemplateScriptException("Error occurred while running the script", e);
        }
    }
}
