package sogei.utility;

 /**
 * Classe che implementa e svolge l'algoritmo di controllo di validità della partita iva
 * e del codice fiscale delle persone non fisiche confrontando anche il valore del check-digit
 * la classe restituisce un codice di ritorno che può assumere dei valori predeterminati
 *
 * Downloaded from http://www.agenziaentrate.gov.it/wps/content/Nsilib/Nsi/Home/CosaDeviFare/Richiedere/Codice+fiscale+e+tessera+sanitaria/Richiesta+TS_CF/SchedaI/Programma+correttezza+formale+CF/
 * @author Sogei S.p.A.
 */
 public class UCheckNum {
 private String strCfNum;
 private int pintCodUff; 

 /**
 * Costruttore parametrico che inizializza l'oggetto con la stringa che gli viene passata come parametro
 */
 public UCheckNum (String s ) {
 strCfNum = s;
 }
 /**
 * Metodo che gestisce il caso di una partita iva o di un codice fiscale di una persona non fisica
 * @return char
 * @param p java.lang.String
 */
 public String cfNum (String p ) {
 String strRetcode = null;
 if (Integer.parseInt(p.substring(0,7)) < 8000000)
 {strRetcode = "2";} //partita iva o codice fiscale delle p.n.f.
 else if (pintCodUff > 95)
 {strRetcode = "N";} //codice ufficio errato per c.f. delle p.n.f.
 else
 {strRetcode = "5";} //c.f. p.n.f. non contribuenti iva

 if ((controlCfNum(p.substring(0,11)) == false) && (strRetcode == "2" ))
 {strRetcode = "Q";} //check-digit errato
 else if (controlCfNum(p.substring(0,11)) == false)
 {strRetcode = "T";} //check-digit errato


 return strRetcode;
 }
 /**
 * Metodo pubblico di codifica del return code che acquisisce una stringa contenente un solo carattere
 * al quale corrisponde una stringa di dettaglio sul codice di ritorno
 * 
 */
 public String codificaRetCodeCfNum ( char chrValore ) {
 String strRetcode = null; //codice di ritorno

 switch(chrValore) // viene attribuito ad ogni valore ricevuto un significato
 {
 case '0':
 strRetcode = "NUMERO DI CODICE FISCALE DEFINITIVO";
 break;

 case '1':
 strRetcode = "NUMERO DI CODICE FISCALE PROVVISORIO ASSEGNATO D'UFFICIO (DU.74 DU.75 101.75)";
 break;

 case '2':
 strRetcode = "NUMERO DI CODICE FISCALE DELLE P.N.F. OPPURE NUMERO DI PARTITA I.V.A.";
 break;

 case '3':
 strRetcode = "NUMERO DI CODICE FISCALE PROVVISORIO ASSEGNATO DALLE INTENDENZE DI FINANZE";
 break;

 case '4':
 strRetcode = "NUMERO DI CODICE FISCALE PROVVISORIO ASSEGNATO DALLE IMPOSTE DIRETTE";
 break;

 case '5':
 strRetcode = "NUMERO DI CODICE FISCALE DELLE P.N.F. NON CONTRIBUENTI I.V.A.";
 break;

 case '6':
 strRetcode = "NUMERO DI CODICE FISCALE DEFINITIVO P.F. SOPRAELEVATA DI OMOCODICE";
 break;

 case 'A':
 strRetcode = "NUMERO DI CODICE FISCALE DEFINITIVO CON CHECK DIGIT ERRATO";
 break;

 case 'B':
 strRetcode = "NUMERO DI CODICE FISCALE DEFINITIVO FORMALMENTE ERRATO";
 break;

 case 'I':
 strRetcode = "NUMERO DI CODICE FISCALE A 11 CIFRE NON ALLINEATO";
 break;

 case 'L':
 strRetcode = "NUMERO DI CODICE FISCALE A 11 CIFRE NON NUMERICO";
 break;

 case 'M':
 strRetcode = "NUMERO DI CODICE FISCALE A 11 CIFRE AVENTE LE PRIME 7 CIFRE A ZERO";
 break;

 case 'N':
 strRetcode = "NUMERO DI CODICE FISCALE A 11 CIFRE CON NUMERO DI CODICE UFFICIO ERRATO";
 break;

 case 'O':

 strRetcode = "NUMERO DI CODICE FISCALE PROVVISORIO ASSEGNATO D'UFFICIO AVENTE PROGRESSIVO NUMERICO NON AMMISSIBILE (FUORI RANGE)";
 break;

 case 'P':
 strRetcode = "NUMERO DI CODICE FISCALE PROVVISORIO ASSEGNATO D'UFFICIO (DU.75 101.75) CON CHECK DIGIT ERRATO";
 break;

 case 'Q':
 strRetcode = "NUMERO DI CODICE FISCALE DELLE P.N.F. OPPURE NUMERO DI PARTITA I.V.A. CON CHECK DIGIT ERRATO";
 break;

 case 'R':
 strRetcode = "NUMERO DI CODICE FISCALE PROVVISORIO ASSEGNATO DALLE II.FF. CON CHECK DIGIT ERRATO";
 break;

 case 'S':
 strRetcode = "NUMERO DI CODICE FISCALE PROVVISORIO ASSEGNATO DALLE II.DD. CON CHECK DIGIT ERRATO";
 break;

 case 'T':
 strRetcode = "NUMERO DI CODICE FISCALE DELLE P.N.F. NON CONTRIBUENTI I.V.A. CON CHECK DIGIT ERRATO";
 break;

 default:
 break;
 } 
 return strRetcode;
 }
 /**
 * Metodo che esegue sulla stringa partita iva il controllo sul check-digit calcolando 
 * il complemento a dieci dell'ultimo carattere a destra 
 */
 public boolean controlCfNum (String s) {

 Integer intAppoggio; 
 int pintAppo;
 int pintUltimoCarattere;
 int pintTotale = 0;
 //somma dei valori attribuiti alle cifre pari 
 for (int i=1; i<11; i+=2)
 {
 String strElem = getCfNum().substring(i,i+1);
 intAppoggio = Integer.valueOf(strElem); //da una singola cifra della stringa passo ad un integer
 pintAppo = (intAppoggio.intValue())*2; //passaggio ad un intero per moltiplicarlo
 String strS2 = String.valueOf(pintAppo); // passo di nuovo a stringa per dividerla in due parti senza utilizzare un array
 for (int j=0; j<strS2.length(); j++)
 {String strElem1 = strS2.substring(j,j+1);
 pintTotale += Integer.parseInt(strElem1); //sommo il valore delle due cifre della stringa della somma
 }
 } 
 //somma dei valori attribuiti alle cifre dispari
 for (int k=0; k<9; k+=2)
 {String strElem2 = getCfNum().substring(k,k+1);
 pintTotale += Integer.parseInt(strElem2);
 }
 // estraggo l'ultima cifra della partita iva
 String strElem = getCfNum().substring(10,11);
 intAppoggio = Integer.valueOf(strElem);
 pintUltimoCarattere = (intAppoggio.intValue());
 // controllo del complemento a dieci della cifra a destra del totale 
 if (pintTotale % 10 == 0)
 { 
 return (pintTotale % 10) == pintUltimoCarattere;
 }
 else
 {
 return (10 - (pintTotale % 10)) == pintUltimoCarattere; 
 } 
 } 
 /**
 * @return boolean
 */
 public boolean controllaCfNum() {

 char c = trattCfNum().charAt(0);

 switch (c) {
 case '0':
 return true ;
 case '1':
 return true ;
 case '2':
 return true ;
 case '3':
 return true ;
 case '4':
 return true ;
 case '5':
 return true ;
 case 'A':
 return false ;
 case 'B':
 return false ;
 case 'I':
 return false ;
 case 'L':
 return false ;
 case 'M':
 return false ;
 case 'N':
 return false ;
 case 'O':
 return false ;
 case 'P':
 return false ;
 case 'Q':
 return false ;
 case 'R':
 return false ;
 case 'S':
 return false ;
 case 'T':
 return false ;

 }
 return false;
 }
 /**
 * Metodo che ritorna una stringa come partita iva.
 * @return java.lang.String
 */
 public String getCfNum ( ) {
 String s = new String(this.strCfNum);
 return s;
 }
 /**
 * Metodo che inizializza una partita iva prendendo come parametro di passaggio una stringa.
 */
 public void setCfNum (String s1) {
 strCfNum = new String(s1);
 return;
 }
 /**
 * Metodo pubblico di controlli formali sulle parti che compongono la partita iva in maniera specifica
 * sul codice ufficio e sul carattere di controllo;per quanto riguarda i numeri di codice fiscale a 11 cifre
 * oltre al check digit viene controllato anche il codice dell'ufficio; se trattasi di numeri di codice fiscale provvisori
 * assegnati dalle fonti DU.74 DU.75 e 101.75, vengono controllati anche i range dei progressivi assegnati; quindi
 * accede alla routine di attribuzione dei return code da restituire 
 */
 public String trattCfNum () {

 for (int i=0 ; i < 11; i++) //ciclo per vedere se è stata passata una
 //partita iva con un carattere non numerico 
 {
 if (!(Character.isDigit(strCfNum.charAt(i))))
 {return "L";}
 } 
 try //controllo ulteriore sulla numericità del codice ufficio
 {
 pintCodUff = Integer.parseInt(strCfNum.substring(7,10));
 }
 catch(java.lang.RuntimeException exc)
 {
 return "L";
 } 
 if ( strCfNum.substring(0,7) == "0000000" ) //errore 
 {return "M";}

 if ( pintCodUff == 0 ) //controlli sul codice ufficio
 {
 if ((Integer.parseInt(strCfNum.substring(0,7)) > 0) && (Integer.parseInt(strCfNum.substring(0,7)) < 273961))
 {return "1";}
 if ((Integer.parseInt(strCfNum.substring(0,7)) > 0400000) && (Integer.parseInt(strCfNum.substring(0,7)) < 1072480 ) ||
 (Integer.parseInt(strCfNum.substring(0,7)) > 1500000) && (Integer.parseInt(strCfNum.substring(0,7)) < 1828637 ) ||
 (Integer.parseInt(strCfNum.substring(0,7)) > 2000000) && (Integer.parseInt(strCfNum.substring(0,7)) < 2054096 ))
 { if (controlCfNum(strCfNum))
 {return "1";}
 else 
 {return "P";}
 }
 else
 {return "O";}

 }

 else if (pintCodUff == 999) 
 { 
 return cfNum(strCfNum);
 } 

 else if (((pintCodUff > 0) && (pintCodUff < 101)) || ((pintCodUff > 119) && (pintCodUff < 122))||(pintCodUff == 888)) 
 { 
 return cfNum(strCfNum);
 }
 else if ((pintCodUff > 150) && (pintCodUff < 246))
 {
 if (controlCfNum(strCfNum)) //controllo sul check digit 
 {return "3";}
 else 
 {return "R";}
 }

 else if ((pintCodUff > 300) && (pintCodUff < 767))

 { if (controlCfNum(strCfNum.substring(0,11)))
 {return "4";}
 else 
 {return "S";}
 }
 else if ((pintCodUff > 899) && (pintCodUff < 951))

 { if (controlCfNum(strCfNum.substring(0,11)))
 {return "4";}
 else 
 {return "S";}
 }
 else
 {return "N";}

 }
 }
