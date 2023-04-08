package io.github.shafthq.shaft.tools.tms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.shaft.cli.FileActions;
import com.shaft.tools.io.ReportManager;
import io.github.shafthq.shaft.tools.io.helpers.ReportManagerHelper;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

import static io.restassured.RestAssured.*;
import static io.restassured.config.EncoderConfig.encoderConfig;


public class XrayIntegrationHelper {
    private static String getLinkJIRATicketRequestBody() {
        return """
                {
                   "update":{
                     "issuelinks":[
                       {
                         "add":{
                           "type":{
                             "name":"Relates"
                           },
                           "outwardIssue":{
                             "key":"${TICKET_ID}"
                           }
                         }
                       }
                     ]
                   }
                 }
                """;
    }
}
