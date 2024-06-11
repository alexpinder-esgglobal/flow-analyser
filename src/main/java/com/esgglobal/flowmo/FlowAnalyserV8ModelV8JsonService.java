package com.esgglobal.flowmo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;
import uk.co.utilisoft.v8model.base.Errors;
import uk.co.utilisoft.v8model.base.Header;
import uk.co.utilisoft.v8model.base.Trailer;
import uk.co.utilisoft.v8model.gas.nexus.NexusHeader;
import uk.co.utilisoft.v8model.gas.nexus.NexusTrailer;
import uk.co.utilisoft.v8model.gas.proteus.ProteusHeader;
import uk.co.utilisoft.v8model.gas.rgma.RgmaHeader;
import uk.co.utilisoft.v8model.gas.rgma.RgmaTrailer;
import uk.co.utilisoft.v8model.gas.transco.TranscoHeader;
import uk.co.utilisoft.v8model.gas.transco.TranscoTrailer;
import uk.co.utilisoft.v8model.generator.flow.GenericFlow;
import uk.co.utilisoft.v8model.json.V8ModelFailedToBindException;
import uk.co.utilisoft.v8model.json.V8ModelV8JsonResult;
import uk.co.utilisoft.v8model.json.V8ModelV8JsonService;

public class FlowAnalyserV8ModelV8JsonService extends V8ModelV8JsonService {

  public FlowAnalyserV8ModelV8JsonService(String aV8RestUrl) {
    super(aV8RestUrl);
  }

  public V8ModelV8JsonResult bindFlow(String aJsonFlow, Industry industry) throws V8ModelFailedToBindException {
    Errors errors = null;
    Header header = null;
    Trailer trailer = null;

    try {
      JsonObject flowJsonObject = new JsonParser().parse(aJsonFlow).getAsJsonObject();

      JsonElement errorsObject = flowJsonObject.get("errors");

      if (errorsObject != null) {
        errors = new Gson().fromJson(errorsObject, Errors.class);
      }

      JsonObject headerJsonObject = flowJsonObject.get("header").getAsJsonObject();

      java.util.Map<String, String> headerFieldsByType = new HashMap<>();

      for (JsonElement headerItem : headerJsonObject.get("items").getAsJsonArray()) {

        String headerItemValue = headerItem.getAsJsonObject().get("text").getAsString();

        if (StringUtils.isNotEmpty(headerItemValue)) {
          headerFieldsByType.put(headerItem.getAsJsonObject().get("type").getAsString(), headerItemValue);
        }
      }

      String flowType = flowJsonObject.get("flow").getAsString();
      GenericFlow flow;

      if (Industry.DTC.equals(industry)) {
        // e.g. AREGI005 split into 005
        String flowVersion = headerFieldsByType.get("FLOWID").substring(5, 8);

        Class<?> clazz =
          Class.forName("uk.co.utilisoft.v8model.dtc.generated.flow." + "Flow_" + flowType + "_" + flowVersion);

        flow = (GenericFlow) clazz.newInstance();
      }
      else {
        // All gas flows are implicitly version 001
        Class<?> clazz = Class.forName("uk.co.utilisoft.v8model.gas.generated.flow." + "Flow_" + flowType + "_001");

        flow = (GenericFlow) clazz.newInstance();
      }

      if (Industry.DTC.equals(industry)) {

        uk.co.utilisoft.v8model.dtc.Header dtcHeader = new uk.co.utilisoft.v8model.dtc.Header();

        dtcHeader.setFileIdentifier(headerFieldsByType.get("MSGID"));
        dtcHeader.setFileType(headerFieldsByType.get("FLOWID"));
        dtcHeader.setFromRoleCode(headerFieldsByType.get("FROMROLE"));
        dtcHeader.setFromParticipantId(headerFieldsByType.get("FROMID"));
        dtcHeader.setToRoleCode(headerFieldsByType.get("TOROLE"));
        dtcHeader.setToParticipantId(headerFieldsByType.get("TOID"));
        dtcHeader.setCreationTime(headerFieldsByType.get("DATETIME"));
        dtcHeader.setSendingApplicationId(headerFieldsByType.get("SENDAPPID"));
        dtcHeader.setReceivingApplicationId(headerFieldsByType.get("RECAPPID"));
        dtcHeader.setBroadcast(headerFieldsByType.get("BROADCAST"));
        dtcHeader.setTestDataFlag(headerFieldsByType.get("TESTFLAG"));

        header = dtcHeader;
      }
      else {

        if (flow.getHeader() instanceof RgmaHeader) {
          RgmaHeader rgmaHeader = new RgmaHeader();

          rgmaHeader.setFlowId(headerFieldsByType.get("FLOWID"));
          rgmaHeader.setOriginatorId(headerFieldsByType.get("ORIGINATORID"));
          rgmaHeader.setOriginatorRoleCode(headerFieldsByType.get("ORIGINATORROLECODE"));
          rgmaHeader.setRecipientId(headerFieldsByType.get("RECIPIENTID"));
          rgmaHeader.setRecipientRoleCode(headerFieldsByType.get("RECIPIENTROLECODE"));
          rgmaHeader.setCreationDate(headerFieldsByType.get("CREATIONDATE"));
          rgmaHeader.setCreationTime(headerFieldsByType.get("CREATIONTIME"));
          rgmaHeader.setFileIdentifier(headerFieldsByType.get("FILEIDENTIFIER"));
          rgmaHeader.setFileUsageCode(headerFieldsByType.get("FILEUSAGECODE"));
          rgmaHeader.setRecordCount(Integer.parseInt(headerFieldsByType.get("RECORDCOUNT")));
          rgmaHeader.setTransactionCount(Integer.parseInt(headerFieldsByType.get("TRANSACTIONCOUNT")));

          header = rgmaHeader;
        }
        else if (flow.getHeader() instanceof TranscoHeader){
          TranscoHeader transcoHeader = new TranscoHeader();

          transcoHeader.setOrganisationId(headerFieldsByType.get("ORGANISATIONID"));
          transcoHeader.setFlowId(headerFieldsByType.get("FLOWID"));
          transcoHeader.setCreationDate(headerFieldsByType.get("CREATIONDATE"));
          transcoHeader.setCreationTime(headerFieldsByType.get("CREATIONTIME"));
          transcoHeader.setGenerationNumber(headerFieldsByType.get("GENERATIONNUMBER"));

          header = transcoHeader;
        }
        else if (flow.getHeader() instanceof ProteusHeader){
          ProteusHeader proteusHeader = new ProteusHeader();

          proteusHeader.setOrganisationCode(headerFieldsByType.get("ORGANISATIONCODE"));
          proteusHeader.setType(headerFieldsByType.get("FLOWID"));
          proteusHeader.setCreationDate(headerFieldsByType.get("CREATIONDATE"));
          proteusHeader.setCreationTime(headerFieldsByType.get("CREATIONTIME"));
          proteusHeader.setGenerationNumber(headerFieldsByType.get("GENERATIONNUMBER"));

          header = proteusHeader;
        }
        else if (flow.getHeader() instanceof NexusHeader){
          NexusHeader nexusHeader = new NexusHeader();

          nexusHeader.setOrganisationId(headerFieldsByType.get("ORIGINATORID"));
          nexusHeader.setType(headerFieldsByType.get("FLOWID"));
          nexusHeader.setCreationDate(headerFieldsByType.get("CREATIONDATE"));
          nexusHeader.setCreationTime(headerFieldsByType.get("CREATIONTIME"));
          nexusHeader.setGenerationNumber(headerFieldsByType.get("GENERATIONNUMBER"));

          header = nexusHeader;
        }
      }

      JsonObject trailerJsonObject = flowJsonObject.get("trailer").getAsJsonObject();

      Map<String, String> trailerFieldsByType = new HashMap<>();

      for (JsonElement trailerItem : trailerJsonObject.get("items").getAsJsonArray()) {

        String trailerItemValue = trailerItem.getAsJsonObject().get("text").getAsString();

        if (StringUtils.isNotEmpty(trailerItemValue)) {
          trailerFieldsByType.put(trailerItem.getAsJsonObject().get("type").getAsString(),
                                  trailerItemValue);
        }
      }

      //Trailer trailer = new Trailer();

      if (Industry.DTC.equals(industry)) {

        uk.co.utilisoft.v8model.dtc.Trailer dtcTrailer = new uk.co.utilisoft.v8model.dtc.Trailer();

        dtcTrailer.setFileIdentifier(trailerFieldsByType.get("MSGID"));
        dtcTrailer.setGroupCount(trailerFieldsByType.get("TOTALGROUP"));
        dtcTrailer.setCheckSum(trailerFieldsByType.get("CHECKSUM"));
        dtcTrailer.setFlowCount(trailerFieldsByType.get("TOTALFLOW"));
        dtcTrailer.setCreationTime(trailerFieldsByType.get("DATETIME"));

        trailer = dtcTrailer;
      }
      else
      {
        if(flow.getTrailer() instanceof RgmaTrailer) {

          RgmaTrailer rgmaTrailer = new RgmaTrailer();

          trailer = rgmaTrailer;
        }
        else if(flow.getTrailer() instanceof TranscoTrailer) {

          TranscoTrailer transcoTrailer = new TranscoTrailer();

          transcoTrailer.setGroupCount(trailerFieldsByType.get("TOTALGROUP"));

          trailer = transcoTrailer;
        }
        else if(flow.getTrailer() instanceof NexusTrailer) {

          NexusTrailer nexusTrailer = new NexusTrailer();

          nexusTrailer.setGroupCount(trailerFieldsByType.get("TOTALGROUP"));

          trailer = nexusTrailer;
        }


      }



      flow.setHeader(header);
      flow.setTrailer(trailer);

      flow.setErrors(errors);

      JsonArray topLeveGroups = flowJsonObject.get("groups").getAsJsonArray();

      for (JsonElement topLevelGroup : topLeveGroups) {
        flow.getGroups().add(generateGroups(flow, null, topLevelGroup, industry));
      }

      return new V8ModelV8JsonResult(flow, aJsonFlow, errors);
    }
    catch (Exception ex) {
      throw new V8ModelFailedToBindException("Exception while attempting to bind flow from json", ex, errors, header,
                                             aJsonFlow);
    }
  }

}
