package com.trading.sim.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class UiResourceTest {

  @Test
  void mainViewIsWellFormedXml() throws Exception {
    assertXmlResourceIsWellFormed("/fxml/MainView.fxml");
  }

  @Test
  void orderDialogIsWellFormedXml() throws Exception {
    assertXmlResourceIsWellFormed("/fxml/OrderDialog.fxml");
  }

  @Test
  void currencyLabelsEscapeFxmlExpressions() throws Exception {
    assertFalse(readResource("/fxml/MainView.fxml").contains("text=\"$"));
    assertFalse(readResource("/fxml/OrderDialog.fxml").contains("text=\"$"));
  }

  private void assertXmlResourceIsWellFormed(String resourcePath) throws Exception {
    try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
      assertNotNull(stream, resourcePath + " must exist");
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }

  private String readResource(String resourcePath) throws Exception {
    try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
      assertNotNull(stream, resourcePath + " must exist");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
