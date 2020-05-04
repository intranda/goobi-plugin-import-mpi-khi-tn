<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet 
	version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:lxslt="http://xml.apache.org/xslt"
	xmlns:result="http://www.example.com/results"
	extension-element-prefixes="result"
>
<!-- All this is for making Inline Javascript possible! -->

<xsl:output method="html" doctype-system="http://www.w3.org/TR/html4/loose.dtd" doctype-public="-//W3C//DTD HTML 4.0 Transitional//EN" encoding="utf-8"/>


<!-- Transformation done by Wolfram Zieger for the KHI Translatio Nummorum Project in 2010 -->
<!-- Changes:

	Oktober 2011
		- Digilib Links
		- BildnamenPrefixes per Variable - damit können Bilder auch in der Prefinal angezeigt werden

	Januar 2011
		- UC (unknown Charakters) eingefügt - erstmal nur in einer Größe, eventuell machen wir da noch einen
		  "big" Parameter oder etwas in der Art
	
	Oktober 2010 - Teil2
		- cm Link eingeführt (Census Monumentum Link - fast wie der alte <rs> link nur diesmal mit Verweis auf 
		  die CensusID in der monumentum.xml anstatt auf den "key") - soll alle "<rs> links ersetzen...
		- Linkcontainer/ptr ist raus, da das EinsprungAdressen für den Census sind, die *nichts* 
		  im Linkcontainer verloren haben und eigentlich gar nicht visualisert werden müssen (ausser dann,
		  wenn jemand vom Census zu uns springt)
		- <rs> Link raus, da es nur noch <Linkcontainer/rs> gibt - und selbst das ist bereits obsolet/deprecated,
		  weil rs komplett mit <Linkcontainer/cm> ersetzt wird.
		- Einige Textformulierungen (u.a. in Titles) umgestellt
	Oktober 2010 - Teil1
		- CSS : Einbinden von DejaVuSans (vom KHI mitbeeinflußte OpenSource Schriftart)
		- -	Da auf Safari 5 das "faken" von Bold und Italic ganz schlimm aussieht (ist in der Basis
			Schriftart nicht enthalten), sollte in diesem Fall auch die entsprechenden Bold-, Italic-
			und Bold/Italic Varianten eingebunden werden können (über 2 MB nur für die Schrift!). 
			Damit das nicht unnötig kompliziert wird, wird die Schriftart-Konfig in ein extra
			CSS ausgelagert -> von dieser extra Font-CSS haben wir dann zwei. Einmal normal (mit dem Basisfont,
			der keine Bold und Italic Varianten beherbergt, die aber von Mozilla, Chrome, ReKonq und
			Opera vernünftig gefaked werden) und einmal ein HQ-Set speziell für Safari 5, wo dann
			die einzelnen Schriftartvarianten explizit eingebunden sind.
			Normal 	= trans_num_font.css
			HighQ	= trans_num_font-hq.css
			Übrigens: IE bis v.8 ist hier nicht erwähnt, weil der eh nicht TrueType einbinden kann. Das
			dafür verwendete EOT-Fontfile basiert auf dem Basis-TTF und der IE rendert Italic und Bold 
			sowieso ordentlich daraus (immmerhin!).
		- XSLT: 
			- Settingszeile oben eingebaut, da sicher noch einige Settings hinzukommen. Momentan kann man
			dort das CSS Set für die Fonts (s.o.) umswitchen, was eigentlich nur für Safari interessant ist,
			so dass dies wahrscheinlich eleganter automatisch per Browser-Type-Detection erledigt werden
			kann. Da es aber auch leichte (für Typographenaugen vielleicht gar schwerwiegende) Unterschiede
			gibt, wenn man bei anderen Browsern als Safari (außer IE) von Normal auf HQ umschaltet, ist das
			manuelle Umschalten vielleicht trotzdem gut. Kann ja auch neben einer automatischen Safari-Erkennung
			mitlaufen (das wäre vielleicht das Beste).
			- -	Technisch basiert das auf einem Formular mit Radiobuttons. Das CSS Href bekommt eine ID mit, 
				damit kann Javascript das CSS Font Set unkompliziert umtauschen, sobald der entsprechenden
				Radiobutton geklickt ist. Im Prinzip könnte man das auch per Einbutton-Toggle machen, aber da
				bin ich gerade zu faul - das hier ist ja nur für interne Zwecke. 
				Der JS-Code dafür befindet sich sauber ausgelagert in der trans_num.js .
			- Jetzt nun doch: Gerade mit der neuen Schrift macht Folgendes doch Sinn: Habe ein Space vor und
			   nach PND/SWD-Links einefügt
	
	Mai 2010
		- Versuch einer Münzcontainer-Implementierung auf Basis automatisch 
		durchnummerierter CSS Frames, die über visibility per JS ein und ausgeschaltet werden
		- Kleiner Bugfix für über mehrere Seiten laufende Schriftstile
		
	April 2010
		- Erste Implementation für Münzcontainer. Problem: Mehrere Container in einer
		Zeile klappen damit nicht
	24. März 2010
		- Muenzsymbole auf 15px runter skaliert (per Variable "SymbolScale")
		- Zeilenhoehe auf 20px hoch gesetzt
			- nun deutlich besserer Lesefluss
	Zwischendrin
		- u.a. MKM (MuenzkabinettLinks) eingefuegt
		- SeitenScanPreviews eingefuegt (dafuer CSS System veraendert)
	19.03.2010
		- OpacID von #00 [id] nach [id]
	
-->


<!-- ///////////  include the ids file //////////// -->

<xsl:variable name="monumentumID" select="document('monumentum.xml')"/>



	<!-- ///////////  Set some more Variables //////////// -->

	<!-- Skalierung von in den Text eingeblendeten Symbolen wie Links in px -->
	<xsl:variable name="SymbolScale"> 
		<xsl:text>15</xsl:text>
	</xsl:variable>

	<xsl:variable name="DigilibUrl">  
		<xsl:text>http://tn.khi.fi.it:8080/greyskinlight/diginew.jsp?fn=/translatio/books/</xsl:text>
		<xsl:value-of select="//opacid"/>
		<xsl:text>/</xsl:text>
	</xsl:variable>
		
	<!-- Höhen-Skalierung von Unknown Charakters ("UC") -->
	<xsl:variable name="UCScale">
		<xsl:text>20</xsl:text>
		<!-- Ganz guter Kompromiss zw. Size und Lesbarkeit. Zwar größer als Text, allerdings sind auch im
		Original einige der UCs deutlich größer gedruckt als der restliche Text; mehr als 50px ist quatsch,
		denn auf diese Height sind die Buchstabenbilder physisch gerechnet -->
	</xsl:variable>


<!-- ............ Achtung - Unterschiede zwischen finalisierten und "beforefinal"-XML-Versionen ............ -->

	<!-- Pfad zu ausgeschnittenen Bildern und Buchseitenscans -->
	<xsl:variable name="IMG_PATH">
		<xsl:text>../digital_obj/books/</xsl:text>  <!-- "../../../digital_obj/books/" für prefinal , "../../digital_obj/books/" für final -->
		<xsl:value-of select="//opacid"/>
		<xsl:text>/</xsl:text>
	</xsl:variable>

	<xsl:variable name="DigilibPrefix">  <!-- Dateinamen-Prefix für Digilib Images  -->
		<xsl:text>obj_</xsl:text> <!-- "obj_img" für Prefinal, "obj_" für final! -->
	</xsl:variable>

	<xsl:variable name="ObjImgPrefix">  <!-- DateinamenPrefix für normale Images  -->
		<xsl:text>screen_obj_</xsl:text> <!-- "screen_obj_img" für Prefinal, "screen_obj_" für final! -->
	</xsl:variable>

	
	<!-- Pfad zu unbekannten Buchstaben (als Bild ausgeschnitten vorliegend) -->
	<xsl:variable name="UC_PATH">
		<xsl:text>../digital_obj/unknown_characters/</xsl:text> <!-- "../../../digital_obj/unknown_characters/" für Prefinal, "../../digital_obj/unknown_characters/" für final! -->
	</xsl:variable>

<!-- ........................ Generierte Variablen ......................................................... -->
	

	<!-- anhand der OPACID den Filenamen dieses Scriptes bestimmen -->
	<xsl:variable name="FILENAME">
		<xsl:value-of select="//opacid"/>
		<xsl:text>_-_transcript.xml</xsl:text>
	</xsl:variable>

	
	

	
	<!-- //////////////////////////////////////////////////////////////////// -->



<xsl:param name="url"></xsl:param>

<!-- Save the Blank Spaces - bin mir nich sicher, ob es nicht *noch* eleganter geht -->
	<xsl:preserve-space elements="p" />


<xsl:template match="/">

	

	<html>
		<head>
			<meta http-equiv="X-UA-Compatible" content="IE=edge" />
			<title><xsl:value-of select="$url"/></title>
			<link id="FONTCHANGER" href="trans_num_font.css" rel="stylesheet" type="text/css" />
			<link href="trans_num.css" rel="stylesheet" type="text/css" />
			<script src="trans_num.js" type="text/javascript"></script>

			<!-- Mimic Internet Explorer 7 war das -->
			
			<!-- ...... Wir erzeugen nummerierte CSS Frames für die Linkcontainer...... -->
			
			<style type="text/css">
				<xsl:for-each select="//linkcontainer"> <!-- "//" steht fuer "ueberall im Dokument" -->

					<xsl:text>#ctr</xsl:text>
					<xsl:number level="any" count="linkcontainer" format="1" />
					<xsl:text>,</xsl:text>
				</xsl:for-each>
				<xsl:text>ctrXYZ {</xsl:text>
				
						position:absolute;
						overflow: visible;
						visibility:hidden;
						background-image:url(container-back.png);
						left:25px;
						width:550px;
						float: left;
						text-align:left;
						margin-left:0px;
						padding: 8px 8px 8px 8px; 
						font-family: DejaVuSans,arial,helvetica;
						font-size:10px;
						font-weight:normal;
						font-style:normal;
						text-decoration:none;
						color:#134;
						border:1px solid;	
						z-index: 100;
					}
				<!-- "ctrXYZ" steht da am Schluss der Liste, weil sonst hinten ein Komma
				stünde und damit das css Konstrukt nicht erkannt würde -->

				<xsl:text>#DigiFrameWindow{</xsl:text>
				
						position:fixed;
						overflow: visible;
						visibility:hidden; /* visibility:visible; */
						background-color:#e0f7ff;
						left:25px;
						width:700px;
						height:650px;
						float: left;
						text-align:left;
						margin-left:0px;
						padding: 8px 8px 8px 8px; 
						font-family: arial,helvetica;
						font-size:10px;
						font-weight:normal;
						font-style:normal;
						text-decoration:none;
						color:#134;
						border:1px solid;	
						z-index: 200;
					}
				
			</style>
			<!-- .... CSS Linkcontainer-Erzeugung Ende ... -->
						

		</head>
		<body >
		
			<!-- ........... The Settings section is below ..........  -->

			<div id="settings">

						<FORM name="SETTINGS" method="GET" target="_top">
							<xsl:attribute name="action">
								<xsl:value-of select="$FILENAME" />
							</xsl:attribute> 
							
							<span id="ThinFrame">
								Font Quality: &#160; &#160; 
								<input 
									type="radio"
									name="fontsetting"
									value="n"
									onClick="fontsettings();"
									checked="checked"
									title="recommended setting for most browsers"
								/> Normal
								<input 
									type="radio"
									name="fontsetting"
									value="h"
									onClick="fontsettings();"
									title="very recommended setting for the Apple Safari browser"
								/> High
							</span>
						</FORM>

			</div>
			
			<!-- ................ Now for the Digilib Image Viewing Functionality  ..................... -->

			<div id="DigiFrameWindow">
				<!-- Close Button -->
				<a style="cursor:pointer;">
					<xsl:attribute name="onclick">
						<xsl:text>HideDigiFrameWindow(</xsl:text>
						<xsl:text>)</xsl:text>
					</xsl:attribute>
					<img src="close.png" alt="Anzeige schließen" title="Anzeige schließen"/> 
				</a>
				
				
				<span id="SysUeberschrift"> &#160;&#160;DigiLib Grafik Window</span>
				
				<iframe id="DigiFrameID" src="http://tn.khi.fi.it:8080/greyskinlight/diginew.jsp?fn=/translatio/books/" width="100%" height="620" name="DigiFrame">
						<p>Ihr Browser kann leider keine eingebetteten Frames anzeigen.</p>
				</iframe>

			</div>



			<!-- ........... Lets cont the pages and do a reference towards them ..........  -->
			
			<div id="pagesheader">
				<span id="sp"><b>PAGES:</b></span>
			</div>
			
			<div id="pages">

				<xsl:for-each select="//br"> <!-- "//" steht fuer "ueberall im Dokument" -->
					
					<a>
						<xsl:attribute name="href">
							<xsl:text>#Page</xsl:text>
							<xsl:number level="any" count="br" format="1" />					
						</xsl:attribute> 
										
						<xsl:attribute name="title">
						
							<xsl:text>Jump to physical page </xsl:text> 
							<xsl:number level="any" count="br" format="1" />
							<xsl:text> which is numbered as page... </xsl:text>  
							<xsl:value-of select="@p"/>
						</xsl:attribute>
						
						<xsl:text>Page </xsl:text>
						<xsl:number level="any" count="br" format="1" />
						<xsl:text> (</xsl:text>
						<xsl:value-of select="@p"/>
						<xsl:text>)</xsl:text>
						<br/>
										
					</a>		

				</xsl:for-each>

			</div>

			<div id="titleheader">
				<span id="sp"><b>KAPITEL:</b></span>
			</div>


			<!-- ........... Lets cont the Titles and do a reference towards them ..........  -->


			
			<div id="titles">

				<xsl:for-each select="//c"> <!-- "//" steht fuer "ueberall im Dokument" -->
					
					<a>
						<xsl:attribute name="href">
							<xsl:text>#Title</xsl:text>
							<xsl:number level="any" count="c" format="1" />					
						</xsl:attribute> 
										
						<xsl:attribute name="title">
						
							<xsl:text>Jump to Title </xsl:text> 
							<xsl:number level="any" count="c" format="1" />
							<xsl:value-of select="@c"/>
						</xsl:attribute>
					
						<!-- Apply Templates bringt an dieser Stelle alle Links mit - das ist Mist -->
						<!-- <xsl:apply-templates /> -->
						<xsl:value-of select="." />
						<br/><br/>
										
					</a>		

				</xsl:for-each>

			</div>

			
			
<!-- ################################################################################# -->
<!-- ################################################################################# -->
<!-- ################################################################################# -->
			
			
			<!-- .......................... Now Parse the text itself ......................  -->
			<div id="superbreit">
				<div id="buch">
								<!--
								<xsl:text>ImagePfad ist... </xsl:text>  
								<xsl:value-of select="$IMG_PATH" />
								<xsl:text> Alles klar?</xsl:text>  
								-->
								
					<xsl:value-of select="document(/files/file/@href)" />
								
				
					<xsl:apply-templates />
				</div>
			</div>
		</body>
	</html>
</xsl:template>


<!-- //// Alles //// -->
<xsl:template match="content">
 <!-- <p style="font-family:Verdana; font-size:12px; color:black"> -->
 <p>
   <xsl:apply-templates />
 </p>
</xsl:template>


<!-- //// Preformated Text //// -->
<xsl:template match="pre">
	<p style="font-weight:bold; font-size:18px; color:grey">
		<xsl:apply-templates />
	</p>
</xsl:template>


<!-- //// Page Break  //// -->
<xsl:template match="br">
	<span style="font-weight:bold; color:#088; font-style:normal;">
		<br/><br/>

		<!-- Lets make some anchor so the pages-reference from above will work -->
		
		<a>
			<xsl:attribute name="name">
				<xsl:text>Page</xsl:text>  
				<xsl:number level="any" count="br" format="1" />
			</xsl:attribute> 		
		</a>		

		<!-- Make sure, the reader gets the pagebreak told :) -->
		<span id="SeitenPreview">
		
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="$IMG_PATH" />
					<xsl:text>page_screen_</xsl:text>
					<xsl:number level="any" count="br" format="0001" />
					<xsl:text>.jpg</xsl:text>
				</xsl:attribute>

				<xsl:attribute name="target">_blank</xsl:attribute>

				<xsl:attribute name="title">
					<xsl:text>Open Page Scan in a separate Window</xsl:text>
				</xsl:attribute>
					
				<img>
					<xsl:attribute name="ALT">
						<xsl:text>Page Scan Thumb Nail</xsl:text>
					</xsl:attribute> 
					<xsl:attribute name="border">
						<xsl:text>0</xsl:text>
					</xsl:attribute> 
					<xsl:attribute name="width">
						<xsl:text>75</xsl:text>
					</xsl:attribute> 
					<xsl:attribute name="style">
						<xsl:text>margin:0px 0px 0px 0px;</xsl:text>
					</xsl:attribute> 

					<xsl:attribute name="src">
						<!-- <xsl:text>file:</xsl:text> -->
						<xsl:value-of select="$IMG_PATH" />
						<xsl:text>page_thumb_</xsl:text>
						<xsl:number level="any" count="br" format="0001" />
						<xsl:text>.jpg</xsl:text>
					</xsl:attribute> 
				</img>
			
			</a>
			
		</span>

		
		<hr/>


		 <table border="0" width="100%">
			<tbody>
				<tr>
					<td style="font-weight:bold; color:#088">
						<xsl:text>  Physical pagenumber: </xsl:text>  
						<xsl:number level="any" count="br" format="1" />
					</td>
					<td style="font-weight:bold; color:#088">
						<xsl:text>Printed pagenumber: </xsl:text>  
						<xsl:value-of select="@p"/>  
					</td>
				</tr>
			</tbody>
		</table>
		<hr/>

	</span>

	<xsl:apply-templates />

</xsl:template>


<!-- //// Section  //// -->
<xsl:template match="sec">
	<hr style="border:1px dashed #489;" />
	<xsl:apply-templates />
</xsl:template>

<!-- //// sp - gesperrt  //// -->
<xsl:template match="sp">
	<span id="sp">
		<!-- <xsl:text>gesperrt</xsl:text>   -->
		<!-- <xsl:value-of select="." /> -->
		<xsl:apply-templates />
	</span>
	
</xsl:template>

<!-- //// i - Kursiv  //// -->
<xsl:template match="i">
	<span id="kursiv">
		<xsl:apply-templates />
	</span>
</xsl:template>


<!-- //// b - fett  //// -->
<xsl:template match="b">
	<span id="b">
		<xsl:apply-templates />
	</span>
</xsl:template>




<!-- //// Linebreak  //// -->
<xsl:template match="lb">	<!--  -->
	<br/>
	<xsl:apply-templates />
</xsl:template>

<!-- //// Titles  //// -->
<xsl:template match="c">	<!--  -->
	<span style="text-align: center;">
			<!-- Lets make some anchor so the pages-reference from above will work -->
		
		<a>
			<xsl:attribute name="name">
				<xsl:text>Title</xsl:text>  
				<xsl:number level="any" count="c" format="1" />
			</xsl:attribute> 		
		</a>		

		<h2>
			<xsl:apply-templates />
		</h2>
	</span>
</xsl:template>


<!-- //// Absatz //// .................................................................. -->
<xsl:template match="p">
 <p>
	<!-- <xsl:value-of select="." /> -->
	
	<xsl:text>&#160; &#160; &#160;</xsl:text> <!-- Aaaaaahhhh &#160; ist ein Non Breaking Space -->
	<xsl:apply-templates />
</p>
</xsl:template>

<!-- //// Linkcontainer //// .......................................................... -->

<xsl:template match="linkcontainer">
	<xsl:text> </xsl:text>
	
	<!-- Container Symbol verlinken -->
	<a style="cursor:pointer;">
		<xsl:attribute name="onclick">
			<xsl:text>ShowContainer(</xsl:text>
			<xsl:number level="any" count="linkcontainer" format="1" />
			<xsl:text>)</xsl:text>
		</xsl:attribute>
		<img src="lupe-small.png" alt="Datenbanklink" title="Datenbanklink"/>		
	</a>
	
	
	<span>
		<xsl:attribute name="id">
			<xsl:text>ctr</xsl:text>
			<!-- die Nummer ermitteln, damit wir den richtigen CSS Frame erwischen! -->
			<xsl:number level="any" count="linkcontainer" format="1" />
		</xsl:attribute>
		
		<!-- Close Button -->
		<a style="cursor:pointer;">
			<xsl:attribute name="onclick">
				<xsl:text>HideContainer(</xsl:text>
				<xsl:number level="any" count="linkcontainer" format="1" />
				<xsl:text>)</xsl:text>
			</xsl:attribute>
			<img src="close.png" alt="Anzeige schließen" title="Anzeige schließen"/> 
		</a>
		
		
		<span id="SysUeberschrift"> &#160;&#160;Verbindungen zu anderen Datenbanken</span>
		<br/><br/>
		
		<!-- <xsl:apply-templates select="linkcontainer/ptr|linkcontainer/MKM|linkcontainer/rs"/>  -->
		<xsl:apply-templates />
		<br/>
	</span>
	<xsl:text> </xsl:text>
</xsl:template>


<!-- //// Fussnote //// ................................................................ -->
<xsl:template match="f">
 
 <p style="font-size:9px; color:black; text-align:right; border-top-width:thin; border-top-color:#AAA; border-top-style:dotted;">

   <xsl:apply-templates />
 </p>
</xsl:template>




<!-- //// Bildinschrift (inziwschen mit Bildanzeige) //// -->
<xsl:template match="img">	
	<div id="Bildinschrift">
		<img>
			<xsl:attribute name="ALT">
				<xsl:text>A Picture from the original book</xsl:text>
			</xsl:attribute> 
			<xsl:attribute name="border">
				<xsl:text>0</xsl:text>
			</xsl:attribute> 
			<xsl:attribute name="width">
				<xsl:text>550</xsl:text>
			</xsl:attribute> 
			<xsl:attribute name="style">
				<xsl:text>margin:0px 0px 8px 0px;</xsl:text>
			</xsl:attribute> 

			<xsl:attribute name="src">
				<!-- <xsl:text>file:</xsl:text> -->
				<xsl:value-of select="$IMG_PATH" />
				<xsl:value-of select="$ObjImgPrefix" />
				<xsl:value-of select="@num"/>
				<xsl:text>.jpg</xsl:text>
			</xsl:attribute> 
		</img>
		<br/>

		<xsl:if test="@digilibpara != ''">
			<!-- <xsl:text>DIGILIB, Yeah!</xsl:text> -->
			<a style="cursor:pointer;">


				<xsl:attribute name="href">
					<xsl:value-of select="$DigilibUrl" />
					<xsl:value-of select="$DigilibPrefix" />
					<xsl:value-of select="@num"/>
					<xsl:text>&amp;</xsl:text>
					<xsl:value-of select="@digilibpara"/>
				</xsl:attribute> 					
				<xsl:attribute name="target">
					<xsl:text>DigiFrame</xsl:text>
				</xsl:attribute>

				<xsl:attribute name="onclick">
					<xsl:text>ShowDigiFrameWindow('</xsl:text>
					<xsl:value-of select="$DigilibUrl" />
					<xsl:value-of select="$DigilibPrefix" />
					<xsl:value-of select="@num"/>
					<xsl:text>&amp;</xsl:text>
					<xsl:value-of select="@digilibpara"/>
					<xsl:text>')</xsl:text>
				</xsl:attribute>
				<xsl:text>Link zur Digilibdarstellung</xsl:text>
			</a>
			<br/>
		</xsl:if>
				
		<xsl:apply-templates />
	</div>

	<div id="Bildunterschrift">
		<sup>
			<xsl:text>Image ID: </xsl:text>  
			<xsl:value-of select="@num"/>
		</sup>
	</div>

</xsl:template>



<!-- //// UCs - Unknown Characters //// -->
<xsl:template match="uc">	
	<img>
		<xsl:attribute name="ALT">
			<xsl:text>Unknown Character</xsl:text>
		</xsl:attribute> 
		<xsl:attribute name="title">
			<xsl:text>Unknown Character</xsl:text>
		</xsl:attribute> 
		<!--
		<xsl:attribute name="border">
			<xsl:text>0</xsl:text>
		</xsl:attribute> 
		-->
		<xsl:attribute name="align">
			<xsl:text>middle</xsl:text> <!-- bottom sieht nicht so gut aus, middle wird aber von Firefox ignoriert -->
		</xsl:attribute> 
		<xsl:attribute name="height">
			<xsl:value-of select="$UCScale" />
		</xsl:attribute> 
		<xsl:attribute name="style">
			<xsl:text>margin:0px 1px 0px 1px;vertical-align: middle; border-style:none;</xsl:text>
		</xsl:attribute> 
		<xsl:attribute name="src">
			<xsl:value-of select="$UC_PATH" />
			<xsl:text>uc_</xsl:text>
			<xsl:value-of select="@num"/>
			<xsl:text>.jpg</xsl:text>
		</xsl:attribute> 
	</img>

	<xsl:apply-templates />

</xsl:template>



<!-- ////////////////////////////// Tables  /////////////////////////////////////// -->


<xsl:template match="table">	
	<table>
		<tbody>
			<xsl:apply-templates />
		</tbody>
	</table>
</xsl:template>


<xsl:template match="tr">	
	<tr>
			<xsl:apply-templates />
	</tr>
</xsl:template>

<xsl:template match="td">	
	<td>
			<xsl:apply-templates />
	</td>
</xsl:template>



<!-- ////////////////////////////// Schlagworte /////////////////////////////////////// -->

<!-- 

Muenzen. ID geht so: 

http://census.bbaw.de/easydb/module/census/jumpto.php?easydb=ck9btvrvgkcl40n08l4vfgoqe0tknld7&eadb_frame=head&pf_language=&ls=0&errColor=%23ff8800&closeWindow=1&jumpto=151693

und

http://census.bbaw.de/easydb/index.php?grid=TopFrameSet_Monument&easydb=ck9btvrvgkcl40n08l4vfgoqe0tknld7&eadb_frame=head&ls=0

&parent_select_id=10007133
<img alt="link to census database" src="file:arrow003.png" title="link to census database">
-->

<xsl:template match="ptr">

		<!-- http://census.bbaw.de/easydb/censusID=######### -->
		<xsl:text> </xsl:text>
		<a>
			<xsl:attribute name="href">
				<xsl:text>http://census.bbaw.de/easydb/censusID=</xsl:text>  
				<xsl:value-of select="@cRef"/>
			</xsl:attribute> 
			
			<xsl:attribute name="target">_blank</xsl:attribute>
			
			<xsl:attribute name="title">Entry Point for a link from the Census Database</xsl:attribute>
			
			<img>
				<xsl:attribute name="ALT">
					<xsl:text>Entry Point for a link from the Census Database</xsl:text>
				</xsl:attribute> 
				<xsl:attribute name="border">
					<xsl:text>0</xsl:text>
				</xsl:attribute> 
				
				<xsl:attribute name="width">
					<xsl:value-of select="$SymbolScale" />
				</xsl:attribute> 						
				<xsl:attribute name="height">
					<xsl:value-of select="$SymbolScale" />
				</xsl:attribute> 						
				
				<xsl:attribute name="src">
					<xsl:text>file:arrow003.png</xsl:text>
				</xsl:attribute> 
				<xsl:attribute name="title">
					<xsl:text>Einsprungpunkt von census database</xsl:text>
				</xsl:attribute> 
			</img>
						
			<xsl:text> </xsl:text>
			<xsl:apply-templates />
			
			
		</a>		

</xsl:template>






<xsl:template match="MKM">

		<!-- http://census.bbaw.de/easydb/censusID=######### -->
		<xsl:text> </xsl:text>
		<a>
			<xsl:attribute name="href">
				<xsl:text>http://www.smb.museum/ikmk/object.php?id=</xsl:text>  
				<xsl:value-of select="@id"/>
			</xsl:attribute> 
			
			<xsl:attribute name="target">_blank</xsl:attribute>
			
			<xsl:attribute name="title">More information about this coin from the Database of the Münzkabinett, Staatliche Museen zu Berlin </xsl:attribute>
			
			<img>
				<xsl:attribute name="ALT">
					<xsl:text>link to Münzkabinett database</xsl:text>
				</xsl:attribute> 
				
				<xsl:attribute name="width">
					<xsl:value-of select="$SymbolScale" />
				</xsl:attribute> 						
				<xsl:attribute name="height">
					<xsl:value-of select="$SymbolScale" />
				</xsl:attribute> 
				
				<xsl:attribute name="border">
					<xsl:text>0</xsl:text>
				</xsl:attribute> 
				<xsl:attribute name="src">
					<xsl:text>file:MKM.png</xsl:text>
				</xsl:attribute> 
			</img>
						
			<xsl:text> </xsl:text>
			<xsl:apply-templates />

			
		</a>		

</xsl:template>



<!-- ####################### Linkcontainer Action! NOW ############################### -->



<xsl:template match="linkcontainer/MKM">

		<!-- http://census.bbaw.de/easydb/censusID=######### -->
		<span style="margin-left: 27px;">
		<a id="KeinStyle">
			<xsl:attribute name="href">
				<xsl:text>http://www.smb.museum/ikmk/object.php?id=</xsl:text>  
				<xsl:value-of select="@id"/>
			</xsl:attribute> 
			
			<xsl:attribute name="target">_blank</xsl:attribute>
			
			<xsl:attribute name="title">More information about this coin from the Database of the Münzkabinett, Staatliche Museen zu Berlin </xsl:attribute>
			
			<img>
				<xsl:attribute name="ALT">
					<xsl:text>link to Münzkabinett database</xsl:text>
				</xsl:attribute> 
				
				<xsl:attribute name="width">
					<xsl:value-of select="$SymbolScale" />
				</xsl:attribute> 						
				<xsl:attribute name="height">
					<xsl:value-of select="$SymbolScale" />
				</xsl:attribute> 
				
				<xsl:attribute name="border">
					<xsl:text>0</xsl:text>
				</xsl:attribute> 
				<xsl:attribute name="src">
					<xsl:text>file:MKM.png</xsl:text>
				</xsl:attribute> 
			</img>
						
			<xsl:text> Informationen der Datenbank des Münzkabinetts der Staatliche Museen zu Berlin</xsl:text>
			<xsl:apply-templates />

			
		</a>		
	</span> <!-- btw IE breaks DIVs which are inside DIVs; thus those spans!/brs -->
	<br/>
</xsl:template>



<xsl:template match="linkcontainer/rs">

	<!-- 
	####################################################################
		ACHTUNG! Seit 10/2010 obsolet! 
		Wird demnächst rausgenommen! Durch <cm> ersetzt!
		DEPRECATED WARNING!
	####################################################################		
	-->
	
	
	<!-- Wahnsinn - daran habe ich bestimmt 8h gesessen!!! -->

		<!-- http://census.bbaw.de/easydb/censusID=######### -->
		<span style="margin-left: 27px;">
		<xsl:variable name="keyhere" select="./@key"/>
		<xsl:variable name="valuehere" select="."/>

		<xsl:for-each select="$monumentumID/register/regEntry">

			<xsl:if test="./@key = $keyhere">
				<xsl:value-of select="$valuehere" />				

				<a style="background-color:transparent;">
					<xsl:attribute name="id">
						<xsl:text>Census_Link</xsl:text>
					</xsl:attribute>
					<xsl:attribute name="href">
						<xsl:text>http://census.bbaw.de/easydb/censusID=</xsl:text>
						<xsl:value-of select="./@censusID"/>
					</xsl:attribute>

					<xsl:attribute name="target">_blank</xsl:attribute>

					<xsl:attribute name="title">Query the Census database for more details about... <xsl:value-of select="./@key"/>
					</xsl:attribute>
					
					
					<img>
						<xsl:attribute name="ALT">
							<xsl:text>link to census database</xsl:text>
						</xsl:attribute>

						<xsl:attribute name="width">
							<xsl:value-of select="$SymbolScale" />
						</xsl:attribute> 						
						<xsl:attribute name="height">
							<xsl:value-of select="$SymbolScale" />
						</xsl:attribute>

						<xsl:attribute name="border">
							<xsl:text>0</xsl:text>
						</xsl:attribute> 
						<xsl:attribute name="src">
							<xsl:text>file:arrow_rs.png</xsl:text>
						</xsl:attribute> 
					</img>
					<xsl:text> Informationen aus der Census Datenbank zum Thema:</xsl:text>
					<br/>
					<span style="margin-left: 50px;">
						<xsl:value-of select="./@key"/>
					</span>
					

				</a>
			</xsl:if>

		</xsl:for-each>

		
	</span> <!-- btw IE breaks DIVs which are inside DIVs; thus those spans!/brs -->
	<br/>

</xsl:template>


<xsl:template match="linkcontainer/cm">

	<!-- 
		Adaptiert von der alten <rs> Methode
		Durchsucht die monumentum.xml nach "censusID"s, die mit den im Transkript angegebenen
		"CID"s übereinstimmen. Der "key" Wert aus der Monumentum.xml wird dann gleich mit angegeben.
	-->
	

		<!-- http://census.bbaw.de/easydb/censusID=######### -->
		<span style="margin-left: 27px;">
		<xsl:variable name="keyhere" select="./@CID"/>
		<xsl:variable name="valuehere" select="."/>

		<xsl:for-each select="$monumentumID/register/regEntry">

			<xsl:if test="./@censusID = $keyhere">
				<xsl:value-of select="$valuehere" />				
				<a style="background-color:transparent;">
					<xsl:attribute name="id">
						<xsl:text>Census_Link</xsl:text>
					</xsl:attribute>
					<xsl:attribute name="href">
						<xsl:text>http://census.bbaw.de/easydb/censusID=</xsl:text>
						<xsl:value-of select="./@censusID"/>
					</xsl:attribute>

					<xsl:attribute name="target">_blank</xsl:attribute>

					<xsl:attribute name="title">Query the Census database for more details about... <xsl:value-of select="./@key"/>
					</xsl:attribute>
					
					
					<img>
						<xsl:attribute name="ALT">
							<xsl:text>link to census database</xsl:text>
						</xsl:attribute>

						<xsl:attribute name="width">
							<xsl:value-of select="$SymbolScale" />
						</xsl:attribute> 						
						<xsl:attribute name="height">
							<xsl:value-of select="$SymbolScale" />
						</xsl:attribute>

						<xsl:attribute name="border">
							<xsl:text>0</xsl:text>
						</xsl:attribute> 
						<xsl:attribute name="src">
							<xsl:text>file:arrow_rs.png</xsl:text>
						</xsl:attribute> 
					</img>
					<xsl:text> Informationen aus der Census Datenbank (ID: </xsl:text>
					<xsl:value-of select="./@censusID"/>
					<xsl:text>) zum Thema:</xsl:text>
					<br/>
					<span style="margin-left: 50px;">
						<xsl:value-of select="./@key"/>
					</span>
					

				</a>
			</xsl:if>

		</xsl:for-each>


		
	</span> <!-- btw IE breaks DIVs which are inside DIVs; thus those spans!/brs -->
	<br/>

</xsl:template>


<!-- ############################################################################### -->


<xsl:template match="legend">
	<br/>
	<div id="legend">		
		<span id="SysUeberschrift">Legende</span><br/>
		<xsl:apply-templates />		
	</div>
	<br/>
</xsl:template>


<!-- ############################################################################### -->


<xsl:template match="legend/cm">

	<!-- 
		Adaptiert von der alten <rs> Methode
		Durchsucht die monumentum.xml nach "censusID"s, die mit den im Transkript angegebenen
		"CID"s übereinstimmen. Der "key" Wert aus der Monumentum.xml wird dann gleich mit angegeben.
	-->
	
		
		<!-- http://census.bbaw.de/easydb/censusID=######### -->
		<xsl:variable name="keyhere" select="./@CID"/>
		<xsl:variable name="valuehere" select="."/>
		<xsl:variable name="diginumhere" select="./@digilibno"/>

		<xsl:for-each select="$monumentumID/register/regEntry">

			<xsl:if test="./@censusID = $keyhere">

				<xsl:value-of select="$diginumhere" />
				<xsl:text>. </xsl:text>
				<xsl:value-of select="$valuehere" />
								
				<a style="background-color:transparent;">
					<xsl:attribute name="id">
						<xsl:text>Census_Link</xsl:text>
					</xsl:attribute>
					<xsl:attribute name="href">
						<xsl:text>http://census.bbaw.de/easydb/censusID=</xsl:text>
						<xsl:value-of select="./@censusID"/>
					</xsl:attribute>

					<xsl:attribute name="target">_blank</xsl:attribute>

					<xsl:attribute name="title">Query the Census database for more details about... <xsl:value-of select="./@key"/>
					</xsl:attribute>
					
<!--					
					<img>
						<xsl:attribute name="ALT">
							<xsl:text>link to census database</xsl:text>
						</xsl:attribute>

						<xsl:attribute name="width">
							<xsl:value-of select="$SymbolScale" />
						</xsl:attribute> 						
						<xsl:attribute name="height">
							<xsl:value-of select="$SymbolScale" />
						</xsl:attribute>

						<xsl:attribute name="border">
							<xsl:text>0</xsl:text>
						</xsl:attribute> 
						<xsl:attribute name="src">
							<xsl:text>file:arrow_rs.png</xsl:text>
						</xsl:attribute> 
					</img>
-->

					<xsl:text> Mehr Informationen in der Census Datenbank (ID: </xsl:text>
					<xsl:value-of select="./@censusID"/>
					<xsl:text>), Thema: </xsl:text>
					<xsl:value-of select="./@key"/>
					

				</a>
			</xsl:if>

		</xsl:for-each>
		
	<br/>

</xsl:template>

<!-- ############################################################################### -->
<xsl:template match="legend/emptylink">

	<xsl:value-of select="@digilibno" />
	<xsl:text>. Münze bislang nicht klassifiziert.</xsl:text>
	

</xsl:template>
<!-- ############################################################################### -->

<xsl:template match="PND">

	<!-- <a href="http://opac.khi.fi.it/cgi-bin/hkhi_de.pl?t_idn=x&pnd=#########> -->
	&#160;<a>

		<xsl:attribute name="id">
			<xsl:text>PND_Link</xsl:text>
		</xsl:attribute>
		<xsl:attribute name="href">
			<xsl:text>http://opac.khi.fi.it/cgi-bin/hkhi_de.pl?t_idn=x&amp;pnd=</xsl:text>  
			<xsl:value-of select="@num"/>
			<xsl:value-of select="@Num"/>
			<xsl:value-of select="@Nu"/>
			<xsl:value-of select="@NUM"/>
			<xsl:value-of select="@NUm"/>
		</xsl:attribute> 
		
		<xsl:attribute name="target">_blank</xsl:attribute>
		
		<xsl:attribute name="title">Click for detailed PND information</xsl:attribute>
		
		<xsl:apply-templates />
		
	</a>&#160;	

</xsl:template>


<xsl:template match="SWD">

		<!-- <a href="http://opac.khi.fi.it/cgi-bin/hkhi_de.pl?t_idn=x&pnd=#########> -->
		&#160;<a>
			<xsl:attribute name="id">
				<xsl:text>SWD_Link</xsl:text>
			</xsl:attribute>
			<xsl:attribute name="href">
				<xsl:text>http://opac.khi.fi.it/cgi-bin/hkhi_de.pl?t_idn=x&amp;swd=</xsl:text>  
				<xsl:value-of select="@Num"/>
				<xsl:value-of select="@num"/>
				<xsl:value-of select="@Num"/>
				<xsl:value-of select="@Nu"/>
				<xsl:value-of select="@NUM"/>
				<xsl:value-of select="@NUm"/>
			</xsl:attribute> 
			
			<xsl:attribute name="target">_blank</xsl:attribute>
			
			<xsl:attribute name="title">Click for detailed SWD information</xsl:attribute>
			
			<xsl:apply-templates />
			
		</a>&#160;	

</xsl:template>







</xsl:stylesheet>
