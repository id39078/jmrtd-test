package org.idcard;
import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;
import org.jmrtd.PACEKeySpec;
import org.jmrtd.PassportService;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.icao.*;

public class Main {
    public static List<PACEInfo> getPACEInfos(Collection<SecurityInfo> securityInfos) {
        List<PACEInfo> paceInfos = new ArrayList<PACEInfo>();

        if (securityInfos == null) {
            return paceInfos;
        }

        for (SecurityInfo securityInfo: securityInfos) {
            if (securityInfo instanceof PACEInfo) {
                paceInfos.add((PACEInfo)securityInfo);
            }
        }

        return paceInfos;
    }

    public static void main(String args[]){

        try {
            CardTerminal terminal = TerminalFactory.getDefault().terminals().list().get(0);
            CardService cs = CardService.getInstance(terminal);
            PassportService ps = new PassportService(cs, 256, 224, false, true);
            ps.open();

            CardAccessFile cardAccessFile = new CardAccessFile(ps.getInputStream(PassportService.EF_CARD_ACCESS));
            Collection<SecurityInfo> securityInfos = cardAccessFile.getSecurityInfos();
            SecurityInfo securityInfo = securityInfos.iterator().next();
            System.out.println("ProtocolOIDString: " + securityInfo.getProtocolOIDString());
            System.out.println("ObjectIdentifier: " + securityInfo.getObjectIdentifier());

            List<PACEInfo> paceInfos = getPACEInfos(securityInfos);
            System.out.println("DEBUG: found a card access file: paceInfos (" + (paceInfos == null ? 0 : paceInfos.size()) + ") = " + paceInfos);

            if (paceInfos != null && paceInfos.size() > 0) {
                PACEInfo paceInfo = paceInfos.get(0);

                PACEKeySpec paceKey = PACEKeySpec.createCANKey("000000"); // the last 6 digits of the DocumentNo

                ps.doPACE(paceKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()));

                ps.sendSelectApplet(true);

                ps.getInputStream(PassportService.EF_COM).read();
            } else {
                System.out.println("Unsuccessfully");
                ps.close();
                return;
            }


            InputStream is1 = null;
            is1 = ps.getInputStream(PassportService.EF_DG1);

            // Basic data
            DG1File dg1 = (DG1File) LDSFileUtil.getLDSFile(PassportService.EF_DG1, is1);
            System.out.println("------------DG1----------");
            System.out.println("DocumentNumber: " + dg1.getMRZInfo().getDocumentNumber());
            System.out.println("Gender: " + dg1.getMRZInfo().getGender());
            System.out.println("DateOfBirth: " + dg1.getMRZInfo().getDateOfBirth());
            System.out.println("DateOfExpiry: " + dg1.getMRZInfo().getDateOfExpiry());
            System.out.println("DocumentCode: " + dg1.getMRZInfo().getDocumentCode());
            System.out.println("IssuingState: " + dg1.getMRZInfo().getIssuingState());
            System.out.println("Nationality: " + dg1.getMRZInfo().getNationality());
            System.out.println("OptionalData1: " + dg1.getMRZInfo().getOptionalData1());
            System.out.println("OptionalData2: " + dg1.getMRZInfo().getOptionalData2());
            System.out.println("PersonalNumber: " + dg1.getMRZInfo().getPersonalNumber());
            System.out.println("PrimaryIdentifier: " + dg1.getMRZInfo().getPrimaryIdentifier());
            System.out.println("SecondaryIdentifier: " + dg1.getMRZInfo().getSecondaryIdentifier());

            is1.close();


        } catch (CardException e) {
            e.printStackTrace();
        } catch (CardServiceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
