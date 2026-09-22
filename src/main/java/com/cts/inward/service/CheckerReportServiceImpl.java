package com.cts.inward.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cts.inward.dao.CheckerReportDao;
import com.cts.inward.dao.CheckerReportDaoImpl;

public class CheckerReportServiceImpl  implements CheckerReportService {

    private CheckerReportServiceImpl() {
    }

    public static CheckerReportServiceImpl of() {
    	
        return new CheckerReportServiceImpl();
    }

    private final CheckerReportDao reportDao =
            CheckerReportDaoImpl.of();

    @Override
    public byte[] generateRrfXml() {

        List<Map<String, Object>> data =  reportDao.getRrfReportData();

        if (data == null || data.isEmpty()) {
            return null;
        }

        List<Long> statusHistoryIds =
                new ArrayList<>();

        for (Map<String, Object> row : data) {

            Object idsObject =
                    row.get("statusHistoryIds");

            if (idsObject instanceof List) {

                @SuppressWarnings("unchecked")
                List<Long> ids =
                        (List<Long>) idsObject;

                statusHistoryIds.addAll(ids);

            } else {

                Object id =
                        row.get("statusHistoryId");

                if (id != null) {

                    statusHistoryIds.add(
                            ((Number) id).longValue());
                }
            }
        }

        try {

            byte[] xml =  buildRrfXml(data);

            reportDao.updateRrfReportGenerated(
                    statusHistoryIds);

            return xml;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error generating RRF XML",
                    e);
        }
    }

    @Override
    public byte[] generateApprovedXml() {

        List<Map<String, Object>> data = reportDao.getApprovedReportData();

        if (data == null || data.isEmpty()) {
            return null;
        }

        List<Long> statusHistoryIds = new ArrayList<>();

        for (Map<String, Object> row : data) {

            Object id =
                    row.get("statusHistoryId");

            if (id != null) {

                statusHistoryIds.add(
                        ((Number) id).longValue());
            }
        }

        try {

            byte[] xml =
                    buildApprovedXml(data);

            reportDao.updateApprovedReportGenerated(
                    statusHistoryIds);

            return xml;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error generating Approved XML",
                    e);
        }
    }

    private byte[] buildRrfXml(
            List<Map<String, Object>> data)
            throws Exception {

        Document document =
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .newDocument();

        Element root = document.createElement("RRFReport");

        document.appendChild(root);


        Map<String, Element> batchMap =  new LinkedHashMap<>();


        for (Map<String, Object> row : data) {

            Object batchIdObject =
                    row.get("batchId");

            // Skip row if batchId is missing
            if (batchIdObject == null) {
                continue;
            }

            String batchId =
                    String.valueOf(batchIdObject);


            Element cheques =
                    batchMap.get(batchId);

            if (cheques == null) {

                Element batch =
                        document.createElement("batch");

                root.appendChild(batch);


            
                appendElement(
                        document,
                        batch,
                        "batchId",
                        batchIdObject);


                cheques = document.createElement("cheques");

                batch.appendChild(cheques);


                batchMap.put(
                        batchId,
                        cheques);
            }


            Element cheque =
                    document.createElement("cheque");

            cheques.appendChild(cheque);


            appendElement(
                    document,
                    cheque,
                    "chequeNo",
                    row.get("chequeNo"));


           
            appendElement(
                    document,
                    cheque,
                    "amount",
                    row.get("amount"));


           
            appendElement(
                    document,
                    cheque,
                    "drawerAccountNo",
                    row.get("drawerAccountNo"));


        
            appendElement(
                    document,
                    cheque,
                    "payeeAccountNo",
                    row.get("payeeAccountNo"));


           
            appendElement(
                    document,
                    cheque,
                    "payeeName",
                    row.get("payeeName"));


            // <drawerName>
            appendElement(
                    document,
                    cheque,
                    "drawerName",
                    row.get("drawerName"));


            // <bankName>
            appendElement(
                    document,
                    cheque,
                    "bankName",
                    row.get("bankName"));


            // <chequeDate>
            appendDateElement(
                    document,
                    cheque,
                    "chequeDate",
                    row.get("chequeDate"));


            // <returnReason>
            appendElement(
                    document,
                    cheque,
                    "returnReason",
                    row.get("returnReason"));


            // <remark>
            appendElement(
                    document,
                    cheque,
                    "remark",
                    row.get("remark"));
        }


        return documentToBytes(document);
    }


    private byte[] buildApprovedXml(
            List<Map<String, Object>> data)
            throws Exception {

        Document document =
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .newDocument();

        Element root =
                document.createElement("ApprovedReport");

        document.appendChild(root);

       
        Map<String, Element> batchMap =
                new LinkedHashMap<>();


        for (Map<String, Object> row : data) {

            Object batchIdObject =
                    row.get("batchId");

            
            if (batchIdObject == null) {
                continue;
            }

            String batchId =
                    String.valueOf(batchIdObject);


          
            Element cheques =
                    batchMap.get(batchId);


           
            if (cheques == null) {

               
                Element batch =
                        document.createElement("batch");

                root.appendChild(batch);


                
                appendElement(
                        document,
                        batch,
                        "batchId",
                        batchIdObject);


               
                cheques =
                        document.createElement("cheques");

                batch.appendChild(cheques);


                batchMap.put(
                        batchId,
                        cheques);
            }


            Element cheque =
                    document.createElement("cheque");

            cheques.appendChild(cheque);


          

            appendElement(
                    document,
                    cheque,
                    "chequeNo",
                    row.get("chequeNo"));


            appendElement(
                    document,
                    cheque,
                    "amount",
                    row.get("amount"));


            appendElement(
                    document,
                    cheque,
                    "accountNumber",
                    row.get("accountNumber"));


            appendElement(
                    document,
                    cheque,
                    "payeeAccountNo",
                    row.get("payeeAccountNo"));


            appendElement(
                    document,
                    cheque,
                    "payeeName",
                    row.get("payeeName"));


            appendElement(
                    document,
                    cheque,
                    "drawerName",
                    row.get("drawerName"));


            appendElement(
                    document,
                    cheque,
                    "bankName",
                    row.get("bankName"));


            appendDateElement(
                    document,
                    cheque,
                    "chequeDate",
                    row.get("chequeDate"));
        }


        return documentToBytes(document);
    }

    private void appendElement(
            Document document,
            Element parent,
            String name,
            Object value) {

        Element element =
                document.createElement(name);


        if (value != null) {

            String text;


            if (value instanceof BigDecimal) {

                text =
                        ((BigDecimal) value)
                                .toPlainString();

            } else {

                text =
                        String.valueOf(value);
            }


            element.appendChild(
                    document.createTextNode(text));
        }


        parent.appendChild(element);
    }



    private void appendDateElement(
            Document document,
            Element parent,
            String name,
            Object value) {

        Element element =
                document.createElement(name);


        if (value instanceof Date) {

            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "yyyy-MM-dd");

            element.appendChild(
                    document.createTextNode(
                            formatter.format(value)));

        } else if (value != null) {

            element.appendChild(
                    document.createTextNode(
                            String.valueOf(value)));
        }


        parent.appendChild(element);
    }

    private byte[] documentToBytes(
            Document document)
            throws Exception {

        Transformer transformer =
                TransformerFactory
                        .newInstance()
                        .newTransformer();


        transformer.setOutputProperty(
                OutputKeys.INDENT,
                "yes");


        transformer.setOutputProperty(
                OutputKeys.ENCODING,
                "UTF-8");


        transformer.setOutputProperty(
                OutputKeys.OMIT_XML_DECLARATION,
                "no");


        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();


        transformer.transform(
                new DOMSource(document),
                new StreamResult(outputStream));


        return outputStream.toByteArray();
    }
}