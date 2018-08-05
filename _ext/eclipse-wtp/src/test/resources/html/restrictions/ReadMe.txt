Various manual tests have been performed with external and internal DTDs.
They seem not to be used by the HTML formatter. Instead they are a 
helper to parse the DOCTYPE and select the right content model.
The DTDParser.parse throws a SAXParseException when parsing the 
HTML DTDs provided with the WST JARs (accessed by the System catalog).
This nominal and expected exception is eaten (DTDParser.currentDTD not set).

Since currently there seems to be no use case for DTD/XSD/catalog,
we omit it for Spotless integration.