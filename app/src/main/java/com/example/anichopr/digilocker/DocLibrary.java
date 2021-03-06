package com.example.anichopr.digilocker;

import android.os.StrictMode;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Created by anichopr on 7/27/2015.
 */
public class DocLibrary {
    public static DigiDoc[] otherDocs = null;
    public static DigiDoc[] essentialDocs = null;
    static DigiDoc[] allDocs = null;

    static String[] importantDocNames = {
            "passport",
            "driving",
            "voter",
            "pan"
    };

    public static void refreshEssentialAndOthersDigiDocs() {
        ArrayList<DigiDoc> otherDocList = new ArrayList<DigiDoc>();
        ArrayList<DigiDoc> essentialDocList = new ArrayList<DigiDoc>();

        // refresh all documents
        refreshDigiDocsFromDocument();

        if (allDocs == null)
            return;
        for (int i=0;i<allDocs.length;i++) {
            boolean fEssentialDoc = false;
            for (int j=0; j<importantDocNames.length;j++) {
                if (allDocs[i].documentName.equals(importantDocNames[j])) {
                    essentialDocList.add(allDocs[i]);
                    fEssentialDoc = true;
                    break;
                }
            }

            if (!fEssentialDoc) {
                otherDocList.add(allDocs[i]);
            }
        }

        otherDocs = otherDocList.toArray(new DigiDoc[otherDocList.size()]);
        essentialDocs = essentialDocList.toArray(new DigiDoc[essentialDocList.size()]);
    }

    private static void refreshDigiDocsFromDocument() {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            Connection.Response res = Jsoup.connect("https://digilocker.gov.in/Signin.aspx")
                    .execute();
            Map<String, String> cookie_map = res.cookies();

            Document doc = res.parse();
            Element viewstateValue = doc.getElementById("__VIEWSTATE");
            Element eventalidationValue = doc.getElementById("__EVENTVALIDATION");
            Element eventargumentValue = doc.getElementById("__EVENTARGUMENT");
            Element eventtargetValue = doc.getElementById("__EVENTTARGET");
            Element viewstateencryptedValue = doc.getElementById("__VIEWSTATEENCRYPTED");
            String seed = doc.getElementById("SitePH_btnLogin").attr("onClick").split("'")[1];
            String ss = viewstateencryptedValue.attr("value");
            System.out.println(seed);


            Connection.Response signin = Jsoup.connect("https://digilocker.gov.in/Signin.aspx").
                    data("__VIEWSTATE", viewstateValue.attr("value")).
                    data("__EVENTVALIDATION", eventalidationValue.attr("value")).
                    data("__EVENTARGUMENT", "").
                    data("__EVENTTARGET", "").
                    data("__VIEWSTATEENCRYPTED", viewstateencryptedValue.attr("value")).
                    data("ToolkitScriptManager1_HiddenField", "").
                    data("ctl00$SitePH$HV1", "").
                    data("ctl00$SitePH$btnLogin", "Sign In").
                    data("ctl00$SitePH$hdnFBEmail", "").
                    data("ctl00$SitePH$hdnFBFName", "").
                    data("ctl00$SitePH$hdnFBID", "").
                    data("ctl00$SitePH$hdnFBLName", "").
                    data("ctl00$SitePH$hdnUserId", "").
                    data("ctl00$SitePH$hdngmail", "").
                    data("cctl00$SitePH$hid_LoginAttempt", "").
                    data("ctl00$SitePH$hid_btnText", "").
                    data("ctl00$SitePH$txtEIDNew", "").
                    data("ctl00$SitePH$txtPassword", PasswordEncryption.GetEncryptedPassword("Indrachand@22", seed)).
                    data("ctl00$SitePH$txtUIDNew", "").
                    data("ctl00$SitePH$txtUserID", "Icchopra").
                    data("ctl00$SitePH$txt_UID", "Aadhaar Number").
                    cookies(cookie_map).
                    method(Connection.Method.POST).
                    execute();

            //	File input = new File("C:\\Users\\israut\\Desktop\\DigiLocker.htm");
            //	Document signin = Jsoup.parse(input, "UTF-8", "https://digilocker.gov.in/");

            ArrayList<DigiDoc> allDocsList = DocLibrary.getDigiDocsFromDocument(signin.parse());
            allDocs = allDocsList.toArray(new DigiDoc[allDocsList.size()]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static ArrayList<DigiDoc> getDigiDocsFromDocument(Document doc) {
        ArrayList<DigiDoc> digiDocArray = new ArrayList<DigiDoc>();
        Elements tableDigiDocs = doc.getElementsByClass("tblFlexi");
        Element table = tableDigiDocs.first();

        //for self uploaded Docs
        table = tableDigiDocs.last();
        boolean notAvailable = table.html().contains("Not Available");

        if (!notAvailable) {
            Elements docRows = table.select("tr");
            for (int i = 1; i < docRows.size(); i++) {
                Elements docrow = docRows.eq(i);
                Elements docColumns = docrow.first().select("td");

                String documentName = docColumns.eq(1).first().text();
                String documentURL = docColumns.select("a[href]").first().attr("href");

                DigiDoc digiDoc = new DigiDoc(documentName, documentURL);

                digiDoc.serialNo = Integer.parseInt(docColumns.eq(0).first().text());
                String date = docColumns.eq(2).first().text();
                DateFormat df = new SimpleDateFormat("dd-mm-yyyy", Locale.ENGLISH);

                try {
                    digiDoc.uploadDate = df.parse(date);
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    System.out.println("during date");
                    e.printStackTrace();
                }

                //doc status
                digiDoc.status = docColumns.eq(3).first().text();

                //share id
                digiDoc.shareId = Integer.parseInt(docColumns.eq(5).toString().split("&quot;")[3]);

                DigiDoc.noOfOtherDocs++;
                digiDocArray.add(digiDoc);

            }
        }
        return digiDocArray;
    }
}
