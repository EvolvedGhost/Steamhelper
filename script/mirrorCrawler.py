import time
from urllib import request

from lxml import etree

if __name__ == "__main__":
    # SteamInfo
    response = request.urlopen("https://keylol.com")
    html = response.read()
    html = html.decode("utf-8")
    xhtml = etree.HTML(html)
    eList = xhtml.xpath('//div[@id="steam_monitor"]')
    outText = ""
    for e in eList:
        outText += etree.tostring(e, encoding='utf-8', pretty_print=True, method='html').decode('utf-8')
    eList = xhtml.xpath("//script[contains(text(), 'steam_monitor')]")
    for e in eList:
        outText += etree.tostring(e, encoding='utf-8', pretty_print=True, method='html').decode('utf-8')
    print(outText)
    outText += "\n<timestamp>" + str(int(time.time())) + "</timestamp>"
    f = open('data/steamInfo.html', 'w')
    f.write(outText)
    f.close()
    # SteamWeek
    response = request.urlopen("https://store.steampowered.com/feeds/weeklytopsellers.xml")
    html = response.read()
    html = html.decode("utf-8")
    html += "\n<timestamp>" + str(int(time.time())) + "</timestamp>"
    f = open('data/steamWeek.html', 'w')
    f.write(html)
    f.close()
