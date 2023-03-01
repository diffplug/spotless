package io.github.shafthq.shaft.tools.tms;



import static io.restassured.RestAssured.*;

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
