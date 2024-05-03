' Script by Wolfram Zieger done in 07/2010 
' Kopiert veraltete XMLs aus dem Translatio Nummorum Projekt in den Veraltet Ordner
' in der "Allgemeine Parameter"-Sektion können die Default Pfade verändert werden!
'Option Explicit
set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")



' #######################################################################################
' Allgemeine Parameter
' #######################################################################################

' .... Pfade --> bitte hinten *kein* abschließendes "\" . Danke

'SOURCEPATH = "U:\Document\scripte\vbscopy\quelle" 	'debug
'TARGETPATH = "U:\Document\scripte\vbscopy\ziel" 	'debug

SOURCEPATH = "B:\Projekte\translatio_nummorum\ProduktivDatenStruktur\transcripts\before_final_xml_xtend"
TARGETPATH = "B:\Projekte\translatio_nummorum\ProduktivDatenStruktur\transcripts\veraltet"









' #######################################################################################
' Eigentlicher Code
' #######################################################################################

' ------------------------ Hinweis zum Startup -------------------------------------------

HINWEIS = ""
HINWEIS = HINWEIS & "Dieses Tool kopiert veraltete Files Ihrer Wahl nach" & VBCR
HINWEIS = HINWEIS & "" & VBCR
HINWEIS = HINWEIS & TARGETPATH & VBCR & VBCR
HINWEIS = HINWEIS & "Gleich kommt eine Dateiauswahl - bitte noch etwas Geduld!" & VBCR

blub = WshShell.Popup(HINWEIS, 10, "Veraltet Wegkopieren") '3s reicht


' ------------------------ Fileauswahl ---------------------------------------------------

'SOURCEFILE = GetMeAFile5 ("XML-Files|*.xml|All Files|*.*" , SOURCEPATH)
SOURCEFILE = GetMeAFile4 (SOURCEPATH, "XML Dateien", "*.xml;*.XML")
' Hmmm - Sloow- oeffnet Winword dafür :-( Denn: Filedialog Spaesse sind vollkommen raus seit
' Vista und Server2008 für VBS - und der auch nur leicht schnellere IE-Filedialog ist sowas 
' von haesslich von hinten durchs Auge und dazu noch viel zu rudimentär - das geht gar nich.
' Ich habe bestimmt einen Tag nach Moeglichkeiten gesucht, einen Filedialog in einem
' nicht compilierten Script (ich will, dass man sieht, was das macht!) aufzureissen.
' Ganz, ganz haesslich von Microsoft (und Powershell ist sowas von eingeigelt, kann man
' auch vergessen, wenn man *schnell* was *einfach bedienbares* fuer den User machen will).
' Mit irgendeinem Hack kriegt man wahrscheinlich auch einen schnellen Filedialog in VBS ab
' Vista hin - aber Google findet nix dazu.


SOURCEeXTENSION = "." & fso.GetExtensionName(SOURCEFILE) 'SOURCEeXTENSION sollte .xml sein
SOURCEnAME = fso.GetFileName(SOURCEFILE)

' ------------------------ Fileauswahl Ende -----------------------------------------------



'OURDATE = YEAR(Date()) & "-" & Month(date()) & "-" & DAY(date())
' Date wo months und days eine leading zero haben, wenn es sich ergibt
OURDATE = DatePart("yyyy",Date) & "-" & Right("0" & DatePart("m",Date), 2) & "-" & Right("0" & DatePart("d",Date), 2) 
'OURDATE = Date()
OURTIME = Time()
OURTIME = OURDATE & "_" & OURTIME
OURTIME = Replace(OURTIME, ":", "-")
OURTIME = Replace(OURTIME, ".", "-")


TARGETNAME = Replace(SOURCEnAME,SOURCEeXTENSION,"")
TARGETNAME = TARGETNAME & "_-_" & OURTIME & SOURCEeXTENSION

'TARGETPATH = TARGETPATH & "\" & TARGETNAME  ' klappt mit meiner Progressbar nich
TARGETCOMPLETE = TARGETPATH & "\" & TARGETNAME 
TARGETOBSOLETE = TARGETPATH & "\" & SOURCEnAME

'MsgBox (SOURCEFILE & VBTAB & OURTIME & VBTAB & TARGETNAME) 'debug
PROOF = ""
PROOF = PROOF & "Wenn Sie jetzt [ok] klicken, wird die Datei...  " & VBCR & VBCR
PROOF = PROOF & SOURCEnAME & VBCR & VBCR
PROOF = PROOF & "nach  " &  VBCR & VBCR
PROOF = PROOF & TARGETCOMPLETE & VBCR & VBCR
PROOF = PROOF & "kopiert. Einverstanden? "


CHECKOK = msgbox(PROOF, vbOKCancel, "Just to check if this is valid...")

' Copy it!!

if CHECKOK = 1 then
	CopyMeBeauty SOURCEFILE,TARGETPATH
	' and rename it
	fso.MoveFile TARGETOBSOLETE , TARGETCOMPLETE
	MsgBox ("Es sollte nun alles erledigt sein. Auf Wiedersehen!")
	' Sorry, im Moment keine Zeit hier Checks einzubauen - muss noch geschehen!
End If


'CopyMeBeauty "U:\Document\scripte\vbscopy\quelle\*.*","U:\Document\scripte\vbscopy\ziel"




' #######################################################################################
' Funktionen
' #######################################################################################

Sub CopyMeBeauty (strSourceFile, strTargetFolder)

	Const FOF_CREATEPROGRESSDLG = &H0&


	Set objShell = CreateObject("Shell.Application")
	Set objFolder = objShell.NameSpace(strTargetFolder) 

	objFolder.CopyHere strSourceFile, FOF_CREATEPROGRESSDLG

	
End Sub 'CopyMeBeauty

' ---------------------------------------------------------------------------------------

Function GetMeAFile (TypeSuggestion, SOURCEPATHNAME)
	' Das laeuft traumhaft in XP aber nicht in Citrix (=Server 2008)

	Set ObjFSO = CreateObject("UserAccounts.CommonDialog")

	ObjFSO.Filter = TypeSuggestion
	ObjFSO.FilterIndex = 1
	ObjFSO.InitialDir = SOURCEPATHNAME

	ALLESKLAR = ObjFSO.ShowOpen
	If ALLESKLAR = False Then
		MsgBox ("Schade: Wäre schön gewesen, Sie hätten ein File ausgewählt... Auf Wiedersehen!")
		Wscript.Quit
	End If 

	SOURCEFILENAME = ObjFSO.FileName
	GetMeAFile = SOURCEFILENAME

End Function 'GetMeAFile

' ---------------------------------------------------------------------------------------


Function GetMeAFile4 (SOURCEPATHNAME, TYPENAME, TYPEeXTENSION)
	
' Achtung! geht nur, wenn Winword oder eventuell Winwordviewer installiert sind
' Laeuft dann auch ab Vista bzw. WinServ2008 - ist aber deutlich langsamer (Word wird im
' Hintergrund gestartet, arrrgh!)
	
	'set the type of dialog box you want to use
	'1 = Open
	'2 = SaveAs
	'3 = File Picker
	'4 = Folder Picker
	Const msoFileDialogOpen = 3

	Set objWord = CreateObject("Word.Application")
	

	'where you want to start looking for files
	'You could use a string like "C:\Somefolder\Somefolder\"
	'I chose to use the desktop folder of whoever was running the script.  On Windows 7 it's "C:\Users\Username\Desktop\"
	'Run "set" from a command prompt to see the available environment variables
	'strInitialPath = WshShell.ExpandEnvironmentStrings("%USERPROFILE%") & "\Desktop\"
	strInitialPath = WshShell.ExpandEnvironmentStrings(SOURCEPATHNAME)

	'set the dialog box to open at the desired folder
	objWord.ChangeFileOpenDirectory(strInitialPath)

	With objWord.FileDialog(msoFileDialogOpen)
	   'set the window title to whatever you want
	   .Title = "Bitte eine Datei auswählen"
	   'I changed this to false because I'm working with one file at a time
	   .AllowMultiSelect = False
	   'Get rid of any existing filters
	   .Filters.Clear
	   'Show only the desired file types
	   'for each desired group of file types, add a "Filters.Add" line with a different description and desired extensions
	   'the dialog box will open using whichever filter is first
	   'you can switch to a different filter from a drop-down list on the dialog box
'	   .Filters.Add "XML Dateien", "*.xls;*.xlsx"
	   .Filters.Add TYPENAME, TYPEeXTENSION
	   .Filters.Add "Alle Dateien", "*.*"
			
	   '-1 = Open the file
	   ' 0 = Cancel the dialog box
	   '-2 = Close the dialog box
	   'If objWord.FileDialog(msoFileDialogOpen).Show = -1 Then  'long form
	   If .Show = -1 Then  'short form
		  'Set how you want the dialog window to appear
		  'it doesn't appear to do anything so it's commented out for now
		  '0 = Normal
		  '1 = Maximize
		  '2 = Minimize
		  'objWord.WindowState = 2

		  'the Word dialog must be a collection object
		  'even though I'm using one file, I had to use a For/Next loop
		  '"File" returns a string containing the full path of the selected file
		
		  'For Each File in objWord.FileDialog(msoFileDialogOpen).SelectedItems  'long form
		  For Each File in .SelectedItems  'short form
			 'Change the Word dialog object to a file object for easier manipulation
			 Set objFile = fso.GetFile(File)
			 'Display the full path to the file
			 'WScript.Echo objFile.Path
			 'Display the path to the folder that the file is in
			 'WScript.Echo objFile.ParentFolder
			 'Display just the name of the file
			 'WScript.Echo objFile.Name
		  Next    
		  GetMeAFile4 = objFile.Path
	   Else
	   End If
	End With

	'Close Word
	
	objWord.Quit
	

End Function 'GetMeAFile4

' ---------------------------------------------------------------------------------------

Function GetMeAFile5 (TypeSuggestion, SOURCEPATHNAME)

	' Klappt ab Vista / Server2008 ist aber ungemein rudimentär
	' und ueberhaupt haesslich im Denkansatz
 
     Dim Result
     Dim IE : Set IE = CreateObject("InternetExplorer.Application")
     With IE
         .Visible = False
         .Navigate("about:blank")
         Do Until .ReadyState = 4 : Loop
         With .Document
             .Write "<html><body><input id='f' type='file'></body></html>"
             With .All.f
                 .Focus
                 .Click
                 Result = .Value
             End With
         End With
         .Quit
     End With
     Set IE = Nothing
    GetMeAFile5 = Result
 
 'MsgBox ChooseFile()
End Function 'GetMeAFile5
