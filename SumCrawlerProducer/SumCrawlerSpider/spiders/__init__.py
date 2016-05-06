# This package will contain the spiders of your Scrapy project
#
# Please refer to the documentation for information on how to create and manage
# your spiders.
import scrapy
import functools
import requests

def _link_is_http(l):
    return l.startswith('https://') or l.startswith('http://')

def _link_is_archive(l):
    return l.endswith('.tar.gz')

class GnuSpider(scrapy.Spider):
    name = "gnu-barrymore"
    allowed_domains = ["gnu.org"]
    start_urls = ["http://www.gnu.org"]
    keyring_url = "http://ftp.gnu.org/gnu/gnu-keyring.gpg"
    visited = set()

    def parse(self, response):

        all_urls = [sel.extract() for sel in response.xpath('//a/@href') if _link_is_http(sel.extract())]
        
        archive_urls = [url for url in all_urls if _link_is_archive(sel.extract())]
        sig_urls = [url+".sig" for url in archive_urls]
        for (archive_url, sig_url) in zip(archive_urls, sig_urls):
            item = SumcrawlerspiderItem()
            item['fileUrl'] = archive_url
            sig_reponse = requests.get(sig_url)
            if sig_response.code == 200:
                item['signature'] = sig_response.text
            else:
                item['signature'] = ''
            item['signatureType'] = 'gpg'
            yield item

        follow_links = [url for url in all_urls if not _link_is_archive(url)]
        for link in follow_links:
            if(not link in self.visited):
                self.visited.add(link)
                yield scrapy.Request(link, callback=self.parse)

