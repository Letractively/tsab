<h1> Sellest veebirakendusest </h1>

<p>
See veebirakendus võimaldab mugavalt sirvida automaatse kõnetuvastuse abil
transkribeeritud kõnesalvestusi.

<p>

Selle rakenduse loomist finantseeris riiklik programm 
<a href="http://www.keeletehnoloogia.ee/">"Eesti keele keeletehnoloogiline tugi (2006-2010)"</a>. 

Rakendus loodi projekti <a href="http://www.phon.ioc.ee/dokuwiki/doku.php?id=projects:tuvastus:tuvastus.et">
Eestikeelse kõnetuvastuse meetodite uurimine ja arendamine</a> raames. Rakenduse implementeerimisel osalesid
TTÜ Küberneetika instituudi <a href="http://www.phon.ioc.ee">foneetika ja kõnetehnoloogia labor</a> ja
<a href="http://codehoop.com/">OÜ Codehoop</a>.

<p>
 
Veebirakenduse lähtekood on saadaval <a href="http://www.gnu.org/licenses/agpl.html">AGPL</a> litsentsi alusel (versioon 3). 
AGPL litsents on sarnane GPL litsensile, kuid lisab klausli, mis nõuab tarkvara abil avaliku teenuse osutajalt lähtekoodi
avaldamist ka teenuse saajatele.   

Veebirakenduse lähtekood on saadaval <a href="http://code.google.com/p/tsab/">siin</a>.

<p>

Salvestuste automaatset transkribeerimist ja regulaarset lisamist süsteemi teostav tarkvara ei ole selle
veebirakenduse osa, ning ei ole hetkel ka vabalt saadaval. Tulevikus on plaanis siiski ka tuvastustehnoloogiat
huvitatutele vabalt levitada. Tuvastustes on kasutatud mitmesugust tarkava, millest tähtsamad on:
<ul>
 <li><a href="http://www-i6.informatik.rwth-aachen.de/rwth-asr/">RWTH ASR</a>: kõnetuvastus
 <li><a href="http://www-speech.sri.com/projects/srilm/">SRILM</a>: statistilise keelemudeli loomine
 <li><a href="http://lium3.univ-lemans.fr/diarization/doku.php/welcome">LIUM_SpkDiarization</a>: kõnesalvestuse jagamine kõnelõikudeks, lõikude grupeerimine kõneleja järgi
</ul>
Enamus kasutatud tarkvarast on vaba lähtekoodiga.
<p>

Kõnetuvastuse akustiliste mudelite treenimiseks on kasutatud hetkel umbes 50 tundi käsitsi märgendatud kõnet.
Keelemudeli loomiseks on kasutatud mitmesuguseid tekstiressursse, millest enamik on saadaval 
Tartu Ülikooli Arvutilingvistika Uurimisrühma <a href="http://www.cl.ut.ee/">kodulehel</a>.

<p>Kõnetuvastuse toimimise ülevaatliku kirjeldusega saab tutvuda <a href="http://www.phon.ioc.ee/dokuwiki/doku.php?id=konetuvastus.et">siin</a>.

 