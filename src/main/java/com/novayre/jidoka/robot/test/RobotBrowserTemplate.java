package com.novayre.jidoka.robot.test;

import com.novayre.jidoka.client.api.execution.IUsernamePassword;
import com.novayre.jidoka.client.lowcode.IRobotVariable;
import org.apache.commons.lang.StringUtils;

import com.novayre.jidoka.browser.api.EBrowsers;
import com.novayre.jidoka.browser.api.IWebBrowserSupport;
import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.IRobot;
import com.novayre.jidoka.client.api.JidokaFactory;
import com.novayre.jidoka.client.api.annotations.Robot;
import com.novayre.jidoka.client.api.multios.IClient;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Browser robot template. 
 */
@Robot
public class RobotBrowserTemplate implements IRobot {

	/**
	 * URL to navigate to.
	 */
	private static final String HOME_URL = "https://docs.google.com";
	
	/** The JidokaServer instance. */
	private IJidokaServer<?> server;
	
	/** The IClient module. */
	private IClient client;
	
	/** WebBrowser module */
	private IWebBrowserSupport browser;

	/** Browser type parameter **/
	private String browserType;

	/**
	 * Action "start"
	 * @throws Exception
	 */
	public void start() throws Exception {
		
		server = (IJidokaServer< ? >) JidokaFactory.getServer();

		client = IClient.getInstance(this);


	}

	/**
	 * Open Web Browser
	 * @throws Exception
	 */
	public void openBrowser() throws Exception  {

		browser = IWebBrowserSupport.getInstance(this, client);

		browserType = server.getParameters().get("Browser");
		
		// Select browser type
		if (StringUtils.isBlank(browserType)) {
			server.info("Browser parameter not present. Using the default browser CHROME");
			browser.setBrowserType(EBrowsers.CHROME);
			browserType = EBrowsers.CHROME.name();
		} else {
			EBrowsers selectedBrowser = EBrowsers.valueOf(browserType);
			browser.setBrowserType(selectedBrowser);
			server.info("Browser selected: " + selectedBrowser.name());
		}

		// Set timeout to 60 seconds
		browser.setTimeoutSeconds(60);

		// Init the browser module
		browser.initBrowser();

		server.setNumberOfItems(1);

	}

	/** Method to get file from local path
	 */
	public void uploadAttachmentsToAppian() throws Exception {
		String DocumentPath =server.getEnvironmentVariables().get("DocumentPath").toString();
		File attachmentsDir = new File(DocumentPath);
		server.debug("Looking for files in: " + attachmentsDir.getAbsolutePath());
		File[] filesToUpload = Objects.requireNonNull(attachmentsDir.listFiles());
		server.setNumberOfItems(filesToUpload.length);
		String document = server.getParameters().get("GoogleDocName").toString();
		String filename = attachmentsDir.getAbsolutePath() +"\\" + document + ".docx";
		File fileUpload = new File(filename);
		String output=uploadFile(fileUpload);

		Map<String, IRobotVariable> variables = server.getWorkflowVariables();
		IRobotVariable rv = variables.get("uploadDocId");
		rv.setValue(output);

	}

	/**
	 * Method to upload
	 */
	private String uploadFile(File file) throws Exception {
		String result;

		// Create a FileEntity for the file
		HttpEntity reqEntity = EntityBuilder.create().setFile(file).build();

		// Create the HTTP POST client and request
		CloseableHttpClient client = HttpClients.createDefault();
		String endpointUpload =server.getEnvironmentVariables().get("endPointUpload").toString();
		HttpPost request = new HttpPost(endpointUpload);

		// Get first credentials for Appian
		IUsernamePassword cred = server.getCredentials("AppianRpaTest1").get(0);

		// Retrieve credentials for the designated entry
		request.addHeader("Appian-API-Key", cred.getPassword());
		request.addHeader("Appian-Document-Name", file.getName());
		request.setHeader("Accept", "application/json");
		request.setEntity(reqEntity);


		// Execute the request
		try (CloseableHttpResponse response = client.execute(request)) {
			result = EntityUtils.toString(response.getEntity());
			server.info("Response" +response);
		}


		server.info(result);
		String value = result.split(":")[1];
		String output = value.split(" -")[0];

		client.close();
		server.info("output:"+output);

		return output;

		//server.setResultProperties(output);

	}
	/**
	 * Navigate to Web Page
	 * @throws Exception
	 */
	public void navigateToWeb() throws Exception  {
		
		server.setCurrentItem(1, HOME_URL);
		
		// Navegate to HOME_URL address
		browser.navigate(HOME_URL);
		browser.waitElement(By.cssSelector(Googledoc.CssSelector.GOOGLE_USERNAME),200);


		//Tihis command is uses to make visible in the desktop the page (IExplore issue)
		if (browserType.equals("IE")) {
			client.clickOnCenter();
			client.pause(3000);
		}
		
		// we save the screenshot, it can be viewed in robot execution trace page on the console
		server.sendScreen("Screen after load page: " + HOME_URL);
	}
	/**
	 * Method to write credentials
	 */
    public void entercredentials() throws Exception {
		IUsernamePassword appianCredentials = server.getCredentials("GoogleDocs").get(0);
		server.info("Login Username: " + appianCredentials.getUsername());
		browser.waitElement(By.cssSelector(Googledoc.CssSelector.GOOGLE_USERNAME),50);
		browser.textFieldSet(By.cssSelector(Googledoc.CssSelector.GOOGLE_USERNAME), appianCredentials.getUsername(), true);
		browser.clickOnElement(By.cssSelector(Googledoc.CssSelector.GOOGLE_NEXT));
		TimeUnit.SECONDS.sleep(7);
		browser.waitElement(By.cssSelector(Googledoc.CssSelector.GOOGLE_PASSWORD), 60);
		browser.textFieldSet(By.cssSelector(Googledoc.CssSelector.GOOGLE_PASSWORD), appianCredentials.getPassword(), true);
		browser.clickOnElement(By.cssSelector(Googledoc.CssSelector.GOOGLE_NEXT));
		try {
			browser.waitElement(By.xpath(Googledoc.CssSelector.GOOGLE_RECOVER), 10);
			browser.clickOnElement(By.xpath(Googledoc.CssSelector.GOOGLE_RECOVER));
			TimeUnit.SECONDS.sleep(10);
			browser.textFieldSet(By.cssSelector(Googledoc.CssSelector.GOOGLE_PHONE), "7708696608", true);
			TimeUnit.SECONDS.sleep(5);
			browser.clickOnElement(By.cssSelector(Googledoc.CssSelector.GOOGLE_NEXT));

		} catch (Exception e) {
			browser.waitElement(By.xpath(Googledoc.CssSelector.GOOGLE_SEARCH), 20);
			server.info("Login Successful");
		}
	}

	/**
	 * Method to search document
	 */

	public void searchdocument() throws Exception{
		String document = server.getParameters().get("GoogleDocName").toString();
		server.info("Document name" + document);
		browser.waitElement(By.xpath(Googledoc.CssSelector.GOOGLE_SEARCH),40);
		browser.textFieldSet(By.xpath(Googledoc.CssSelector.GOOGLE_SEARCH),document,true);
		WebElement search = browser.waitElement(By.xpath(Googledoc.CssSelector.GOOGLE_SEARCH),20);
		search.sendKeys(Keys.ENTER);
	}

	public boolean checkdocuments() throws Exception {

		WebElement empty = browser.waitElement(By.xpath(Googledoc.CssSelector.GOOGLE_SEARCH), 20);
		if (empty.isDisplayed())
		{
			server.info("No documents");
			return false;

		}
		else
		{
			server.info("found");
			return true;
		}

	}

	/**
	 * Method to select document
	 * @return
	 */

	public void selectdocument() throws Exception {

		String document = "//*[@title='" + server.getParameters().get("GoogleDocName").toString() + "']";
		server.info("Document Name to find " + document);
		WebElement resultsDiv = browser.waitElement(By.xpath(document), 20);
		try {
			browser.clickOnElement(By.xpath(document));
			browser.waitElement(By.cssSelector(Googledoc.CssSelector.GOOGLE_DOC_TITLE), 20);
			WebElement search = browser.waitElement(By.xpath(Googledoc.CssSelector.GOOGLE_FILE), 20);
			browser.clickOnElement(By.xpath(Googledoc.CssSelector.GOOGLE_FILE));
			TimeUnit.SECONDS.sleep(5);
			browser.clickOnElement(By.xpath(Googledoc.CssSelector.GOOGLE_DOWNLOAD));
			TimeUnit.SECONDS.sleep(5);
			browser.clickOnElement(By.xpath(Googledoc.CssSelector.GOOGLE_WORD));
			TimeUnit.SECONDS.sleep(5);
			/*Actions builder = new Actions(browser.getDriver());
			builder.keyDown(Keys.TAB);
			search.sendKeys(Keys.TAB);
			TimeUnit.SECONDS.sleep(5);
			search.sendKeys(Keys.ALT + "F");
			TimeUnit.SECONDS.sleep(3);
			search.sendKeys(Keys.ARROW_DOWN, "5");
			TimeUnit.SECONDS.sleep(3);
			search.sendKeys(Keys.ARROW_RIGHT);
			TimeUnit.SECONDS.sleep(3);
			search.sendKeys(Keys.ENTER);
			browser.getDriver().findElement(By.xpath(Googledoc.CssSelector.GOOGLE_SIDE_BAR)).sendKeys(Keys.TAB);*/

		} catch (Exception e) {
			server.info("not found");
		}
		/**
		 *
		 * We use the close method implemented in the driver.
		 * In your robots you should use browser.close()
		 */
	}
		private void close () throws Exception {
			browser.getDriver().close();

		}
	
	/**
	 * Close Browser
	 * @throws Exception
	 */
	public void closeBrowser() throws Exception  {
		browser.getDriver().close();
		server.setCurrentItemResultToOK("Success");
	}

	
	/**
	 * Action "end"
	 * @throws Exception
	 */
	public void end() throws Exception {
	}
	

	
}
