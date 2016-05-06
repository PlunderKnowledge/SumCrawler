# This package will contain the spiders of your Scrapy project
#
# Please refer to the documentation for information on how to create and manage
# your spiders.
import scrapy
import functools
import requests

class GnuSpider(scrapy.Spider):
    name = "gnu-barrymore"
    allowed_domains = ["gnu.org"]
    start_urls = ["http://www.gnu.org"]
    keyring_url = "http://ftp.gnu.org/gnu/gnu-keyring.gpg"

    def parse(self, response):
        archive_urls = [sel.extract() for sel in response.xpath('//a/@href') if sel.extract().endswith('.tar.gz')]
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
