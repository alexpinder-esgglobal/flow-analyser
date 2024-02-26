package com.esgglobal.flowmo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import uk.co.utilisoft.v8model.base.Error;
import uk.co.utilisoft.v8model.base.Errors;
import uk.co.utilisoft.v8model.dtc.Header;
import uk.co.utilisoft.v8model.dtc.Trailer;
import uk.co.utilisoft.v8model.gas.rgma.RgmaHeader;
import uk.co.utilisoft.v8model.gas.rgma.RgmaTrailer;
import uk.co.utilisoft.v8model.generator.flow.GenericFlow;
import uk.co.utilisoft.v8model.generator.group.GenericGroup;
import uk.co.utilisoft.v8model.generator.group.GenericItemField;
import uk.co.utilisoft.v8model.generator.group.GroupException;
import uk.co.utilisoft.v8model.generator.group.ValidSetValueMetaData;
import uk.co.utilisoft.v8model.json.V8ModelUnparseableFlowException;
import uk.co.utilisoft.v8model.json.V8ModelV8JsonResult;
import uk.co.utilisoft.v8model.json.V8ModelV8JsonService;

@Controller
public class FlowController {

  private final V8ModelV8JsonService v8ModelService;

  public FlowController(V8ModelV8JsonService v8ModelService) {
    this.v8ModelService = v8ModelService;
  }

  @GetMapping("/")
  public String inputForm() {
    return "form";
  }

  @PostMapping("/")
  public String processInput(@RequestParam("pipeInput") String pipeInput, Model model) {

    try {
      model.addAttribute("pipeInput", pipeInput.trim());

      V8ModelV8JsonResult v8Model = null;
      if (pipeInput.startsWith("\"HEADR\"")) {
        v8Model = v8ModelService.parse(pipeInput.trim(), V8ModelV8JsonService.Industry.GAS);
        model.addAttribute("energyType", "gas");

        RgmaHeader header = (RgmaHeader) v8Model.getGenericFlow().getHeader();

        List<Map<String, Object>> headerAttributes = new ArrayList<>();
        headerAttributes.add(keyValueFieldToMap("Originator ID", header.getOriginatorId(), 2, 0));
        headerAttributes.add(keyValueFieldToMap("Originator Role", header.getOriginatorRoleCode(), 3, 0));
        headerAttributes.add(keyValueFieldToMap("Recipient ID", header.getRecipientId(), 4, 0));
        headerAttributes.add(keyValueFieldToMap("Recipient Role", header.getRecipientRoleCode(), 5, 0));
        headerAttributes.add(keyValueFieldToMap("Creation Date", header.getCreationDate(), 6, 0));
        headerAttributes.add(keyValueFieldToMap("Creation Time", header.getCreationTime(), 7, 0));
        headerAttributes.add(keyValueFieldToMap("File Identifier", header.getFileIdentifier(), 8, 0));
        model.addAttribute("header", headerAttributes);
      }
      else {
        v8Model = v8ModelService.parse(pipeInput.trim(), V8ModelV8JsonService.Industry.DTC);
        model.addAttribute("energyType", "electricity");
        Header header = (Header) v8Model.getGenericFlow().getHeader();

        List<Map<String, Object>> headerAttributes = new ArrayList<>();
        headerAttributes.add(keyValueFieldToMap("File ID", header.getFileIdentifier(), 1, 0));
        headerAttributes.add(keyValueFieldToMap("From Role Code", header.getFromRoleCode(), 3, 0));
        headerAttributes.add(keyValueFieldToMap("From Participant ID", header.getFromParticipantId(), 4, 0));
        headerAttributes.add(keyValueFieldToMap("To Role Code", header.getToRoleCode(), 5, 0));
        headerAttributes.add(keyValueFieldToMap("To Participant ID", header.getToParticipantId(), 6, 0));
        headerAttributes.add(keyValueFieldToMap("Creation Time", header.getCreationTime(), 7, 0));
        headerAttributes.add(keyValueFieldToMap("Test Flag", header.getTestDataFlag(), 8, 0));
        model.addAttribute("header", headerAttributes);

        Trailer trailer = (Trailer) v8Model.getGenericFlow().getTrailer();

        var trailerIndex = flattenGroups(v8Model.getGenericFlow()).size() + 1;
        List<Map<String, Object>> trailerAttributes = new ArrayList<>();
        trailerAttributes.add(keyValueFieldToMap("File ID", trailer.getFileIdentifier(), 1, trailerIndex));
        trailerAttributes.add(keyValueFieldToMap("Group Count", trailer.getGroupCount(), 2, trailerIndex));
        trailerAttributes.add(keyValueFieldToMap("Checksum", trailer.getCheckSum(), 3, trailerIndex));
        trailerAttributes.add(keyValueFieldToMap("Flow Count", trailer.getFlowCount(), 4, trailerIndex));
        trailerAttributes.add(keyValueFieldToMap("Completion Timestamp", trailer.getCreationTime(), 5, trailerIndex));
        model.addAttribute("trailer", trailerAttributes);
      }

      var parsedFlow = v8Model.getGenericFlow();

      model.addAttribute("flowCounter", parsedFlow.getFlowCounter());
      model.addAttribute("flowVersion", parsedFlow.getFlowVersion());
      model.addAttribute("description", parsedFlow.getFlowDescription());
      model.addAttribute("notes", parsedFlow.getFlowExternalNotes());
      model.addAttribute("name", parsedFlow.mName());
      model.addAttribute("errors", getFlowErrors(v8Model));

      List<Map<String, Object>> groups = new ArrayList<>();

      flattenGroups(parsedFlow).forEach(g -> {
        AtomicInteger columnCounter = new AtomicInteger(1);
        Map<String, Object> map = new HashMap<>();
        var fields = g
          .getItemFields()
          .stream()
          .map(f -> fieldToMap(f, g, groups.size() + 1, columnCounter.getAndIncrement()))
          .toList();
        columnCounter.set(1);
        map.put("fields", fields);
        map.put("flowmoDescription", g.getDescription());
        map.put("flowmoName", g.getGroupCode());
        map.remove("groups");
        map.remove("groupCounter");
        groups.add(map);
      });
      model.addAttribute("groups", groups);

      return "form";
    }
    catch (V8ModelUnparseableFlowException e) {
      model.addAttribute("errors", List.of(e.getMessage()));
      return "form";
    }
  }

  private Map<String, Object> keyValueFieldToMap(String name, String value, int fieldIndex, int groupIndex) {
    Map<String, Object> map = new HashMap<>();
    map.put("name", name);
    map.put("value", value);
    map.put("groupIndex", groupIndex);
    map.put("fieldIndex", fieldIndex);
    return map;
  }

  private Map<String, Object> fieldToMap(GenericItemField field, GenericGroup group, int groupIndex, int fieldIndex) {
    try {
      var fieldMetadata = group.getFieldMetadata(field);
      Map<String, Object> map = new HashMap<>();
      map.put("reference", field.getItemReference());
      map.put("name", field.getItemDescription());
      map.put("optional", !field.getItemOptionality().equals("1"));
      map.put("value", getDataItemValue(field, group));
      map.put("groupIndex", groupIndex);
      map.put("fieldIndex", fieldIndex);
      map.put("description", fieldMetadata.description());
      if (fieldMetadata.validSetValues().length > 0) {
        map.put("validValues", Arrays.stream(fieldMetadata.validSetValues()).collect(Collectors.toMap(
          ValidSetValueMetaData::validSetValue, ValidSetValueMetaData::validSetValueDescription, (v1, v2) -> v1)));
      }
      return map;
    }
    catch (GroupException e) {
      throw new RuntimeException(e);
    }
  }

  private List<GenericGroup> flattenGroups(GenericFlow flow) {
    List<GenericGroup> groups = new ArrayList<>();
    flow.getGroups().forEach(g -> {
      groups.add(g);
      groups.addAll(getChildGroups(g));
    });
    return groups;
  }

  private List<GenericGroup> getChildGroups(GenericGroup group) {
    List<GenericGroup> groups = new ArrayList<>();

    if (group.getGroups() != null) {
      group.getGroups().forEach(g -> {
        groups.add(g);
        if (g.getGroups() != null) {
          groups.addAll(getChildGroups(g));
        }
      });
    }

    return groups;
  }

  private List<String> getFlowErrors(V8ModelV8JsonResult results) {
    Errors errors = results.getGenericFlow().getErrors();

    List<String> flowErrors = new ArrayList<>();
    if (errors != null) {
      flowErrors.addAll(errors.getFlowErrors().stream().map(Error::getMsg).toList());
      flowErrors.addAll(errors.getGroupErrors().stream().map(Error::getMsg).toList());
      flowErrors.addAll(errors.getItemErrors().stream().map(Error::getMsg).toList());
    }
    return flowErrors;
  }

  public Object getDataItemValue(GenericItemField aItemField, GenericGroup group) {
    if (aItemField == null) {
      return null;
    }

    Method method = getGetterMethod(aItemField.getGetterMethodName(), group);
    var result = callGetterMethod(method, group);

    if (result instanceof LocalDate) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
      result = ((LocalDate) result).format(formatter);
    }
    else if (result instanceof LocalTime) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss");
      result = ((LocalTime) result).format(formatter);
    }
    else if (result instanceof LocalDateTime) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
      result = ((LocalDateTime) result).format(formatter);
    }

    return result;
  }

  private Object callGetterMethod(Method aMethod, Object object) {
    Object value = null;

    if (aMethod == null) {
      return value;
    }

    try {
      value = aMethod.invoke(object);
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }

    return value;
  }

  private Method getGetterMethod(String aMethodName, GenericGroup group) {
    try {
      return group.getClass().getMethod(aMethodName);
    }
    catch (NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
