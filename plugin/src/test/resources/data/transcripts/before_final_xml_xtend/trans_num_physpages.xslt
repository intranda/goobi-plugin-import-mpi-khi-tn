<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
 <xsl:output method="xml" indent="yes" encoding="utf-8"/> 
<!-- <xsl:output method="text"> -->

<!-- Transformation done by Wolfram Zieger for the KHI Translatio Nummorum Project in 2010 
	fügt physiche Seitennummern ein - und vielleicht kommt noch mehr dazu
-->

<!-- //////////////////////// Changes: /////////////////

	19.03.2010
		- OpacID von "#00 [id]" nach "[id]"
-->



<!-- Save the Blank Spaces - bin mir nich sicher, ob es nicht *noch* eleganter geht -->

<xsl:preserve-space elements="*"/>




	<xsl:template match="/">
              <!--   <xsl:value-of select="document(/files/file/@href)" /> -->
		<xsl:comment>
			KHI Translatio Nummorum Transcript Extended Format 
		</xsl:comment> 
			                      

				<xsl:apply-templates /> 
		

	</xsl:template>

<!-- alles  -  und zwar XML maessig richtig!!! -->
	<xsl:template match="@* | node()">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>




<!-- //// Page Break  //// -->
<xsl:template match="br">	
	<!-- Lets make physical and logical references -->
   
<!--   <xsl:element name="br">		 -->

	<br>
		<xsl:attribute name="pphys">
			<xsl:text>pphys</xsl:text> 
			<xsl:number level="any" count="br" format="1" />
		</xsl:attribute>
		<xsl:attribute name="p">
			<xsl:value-of select="@p"/>
		</xsl:attribute>
	</br>		
	<xsl:apply-templates />


</xsl:template>


<!-- //// Content Tag kam immer ohne Leerzeile vorher - also einfach nochmal /// -->
<xsl:template match="content">
	
<xsl:text>
</xsl:text>
<content>
	<xsl:apply-templates />
</content>
</xsl:template>	


<!-- //// Image Num //// -->
<xsl:template match="img">
	<!-- Lets make sure those NUM ID values are individuals by placing a "img" in front of em -->

	<img>
		<xsl:attribute name="num">
			<xsl:text>img</xsl:text>
			<xsl:value-of select="@num"/>
		</xsl:attribute>
		<!-- mal sehen ob so all die anderen Attribute erscheinen -->
		<!-- <xsl:apply-templates select="@* | node()"/> -->
		
		<xsl:choose>
			<xsl:when test="@digilibpara!=''">
				<xsl:attribute name="digilibpara">
					<xsl:value-of select="@digilibpara"/>
				</xsl:attribute>
			</xsl:when>
		</xsl:choose>
		
		<xsl:apply-templates />
	</img>


</xsl:template>

<!-- //// Die Schreibweise des Parameters num bei PNDs und SWDs //// -->

<xsl:template match="PND">
	<!--
	Beim automatischen Generieren einer DTD fiel mir das hier auf:
	Nu CDATA #IMPLIED
    NUM CDATA #IMPLIED
    NUm CDATA #IMPLIED
    num CDATA #IMPLIED
    Num CDATA #IMPLIED

	die muessen alle als "num" rueberkommen

	-->

	<PND>
		<xsl:attribute name="num">
			<xsl:value-of select="@num"/>
			<xsl:value-of select="@Num"/>
			<xsl:value-of select="@Nu"/>
			<xsl:value-of select="@NUM"/>
			<xsl:value-of select="@NUm"/>
		</xsl:attribute>
		<xsl:apply-templates />
	</PND>



</xsl:template>

<!-- //// Die selben Fehler also bei den SWDs //// -->


<xsl:template match="SWD">

	<SWD>
		<xsl:attribute name="num">
			<xsl:value-of select="@num"/>
			<xsl:value-of select="@Num"/>
			<xsl:value-of select="@Nu"/>
			<xsl:value-of select="@NUM"/>
			<xsl:value-of select="@NUm"/>
		</xsl:attribute>
		<xsl:apply-templates />
	</SWD>



</xsl:template>


<xsl:template match="opacid" name="replace">
<!-- OPAC IDs hatten meist noch das "#00" von unserer Opac-Ausgabe davor - raus damit -->

	<xsl:param name="string">
		<xsl:value-of select="." />
	</xsl:param>
	 <xsl:param name="from">
		<xsl:text>#00 </xsl:text>
	 </xsl:param>
	 <xsl:param name="to">
		<xsl:text/>
	 </xsl:param>
	 
	 <xsl:choose>
		 <xsl:when test="contains($string, $from)">
			 <xsl:value-of select="substring-before($string, $from)"/>
			 <xsl:copy-of select="$to"/>
			 <xsl:call-template name="replace">
				<xsl:with-param name="string" select="substring-after($string, $from)"/>
				<xsl:with-param name="from" select="$from"/>
				<xsl:with-param name="to" select="$to" />
			 </xsl:call-template>
		 </xsl:when>
		 <xsl:otherwise>
			<opacid>
				<xsl:value-of select="$string" />
			</opacid>
		 </xsl:otherwise>
	</xsl:choose>

<!-- xslt 2 waere so einfach

	<opacid>
		<xsl:value-of select="replace(.,'#00 ','','i')"/>
	</opacid>

-->

</xsl:template>


</xsl:stylesheet>
  