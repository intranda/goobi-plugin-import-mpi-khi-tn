/*
trans_num.js ist Bestandteil der trans_num.xslt Transformation.
Es enthält: 
- Methoden zur Darstellung von Fenstern aus Linkcontainern.
- Methoden zum Wechseln von CSS Sets
Von Wolfram Zieger fürs "Translatio Nummorum" Projekt des KHI Florenz
done in 06/2010
updates in 10/2010; 10/2011
*/

function ShowContainer(CtrNr) {
	OurContainer = "ctr" + CtrNr;
	// alert (OurContainer);
	document.getElementById(OurContainer).style.visibility = 'visible' ;
}

function HideContainer(CtrNr) {
	OurContainer = "ctr" + CtrNr;
	document.getElementById(OurContainer).style.visibility = 'hidden' ;
}

function fontsettings () {
		
		ToChange=document.getElementById("FONTCHANGER");
		// alert (ToChange);
	
		if (document.SETTINGS.fontsetting[0].checked) {
			// alert ("normal");
			ToChange.setAttribute('href','trans_num_font.css');
		} 

		if (document.SETTINGS.fontsetting[1].checked) {
			// alert ("high");
			ToChange.setAttribute('href','trans_num_font-hq.css');
		} 
		

	// document.SETTINGS.submit();
}

function ShowDigiFrameWindow(LinkMe) {
	document.getElementById('DigiFrameWindow').src=LinkMe;
	document.getElementById("DigiFrameWindow").style.visibility = 'visible';
}

function HideDigiFrameWindow() {
	document.getElementById("DigiFrameWindow").style.visibility = 'hidden' ;
}