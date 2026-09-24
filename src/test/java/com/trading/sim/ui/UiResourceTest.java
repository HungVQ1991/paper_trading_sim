package com.trading.sim.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class UiResourceTest {

  @Test
  void mainViewIsWellFormedXml() throws Exception {
    assertXmlResourceIsWellFormed("/fxml/MainView.fxml");
  }

  private void assertXmlResourceIsWellFormed(String resourcePath) throws Exception {
    try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
      assertNotNull(stream, resourcePath + " must exist");
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
