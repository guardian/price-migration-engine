#!/usr/bin/ruby

# encoding: UTF-8

require 'json'

# --------------------------------------------------------------

directory = Dir.pwd

puts "directory: #{directory}"

subscription_filepath = "#{directory}/subscription.json"
account_filepath = "#{directory}/account.json"
invoice_preview_filepath = "#{directory}/invoice-preview.json"

if !File.exist?(subscription_filepath) then
    puts "[error] There doesn't seem to be subscription.json. Aborting."
    exit
end

if !File.exist?(account_filepath) then
    puts "[error] There doesn't seem to be account.json. Aborting."
    exit
end

if !File.exist?(invoice_preview_filepath) then
    puts "[error] There doesn't seem to be invoice-preview.json. Aborting."
    exit
end

# ----------------------------------------------------
# Anonymize subscription
puts "processing subscription"
subscription = JSON.parse(IO.read(subscription_filepath))
[
    "id", 
    "accountId",
    "accountNumber",
    "accountName",
    "invoiceOwnerAccountId",
    "invoiceOwnerAccountNumber",
    "invoiceOwnerAccountName",
    "subscriptionNumber"
].each{|key_name|
    subscription[key_name] = key_name
}
subscription["billToContact"] = nil
subscription["soldToContact"] = nil
File.open(subscription_filepath, "w"){|f| f.puts(JSON.pretty_generate(subscription)) }

# ----------------------------------------------------
# Anonymize account
puts "processing account"
account = JSON.parse(IO.read(account_filepath))
[
    "id",
    "name",
    "accountNumber",
    "crmId",
    "IdentityId__c",
    "sfContactId__c",
    "CreatedRequestId__c"
].each{|key_name|
    account["basicInfo"][key_name] = key_name
}
account["billToContact"] = nil
account["soldToContact"] = {
    "country" => account["soldToContact"]["country"]
}
File.open(account_filepath, "w"){|f| f.puts(JSON.pretty_generate(account)) }

# ----------------------------------------------------
# Anonymize invoice preview
puts "processing invoice preview"
invoice_preview = JSON.parse(IO.read(invoice_preview_filepath))
invoice_preview["accountId"] = "accountId"
invoice_preview["invoiceItems"] = invoice_preview["invoiceItems"]
    .map{|invoiceItem|
        invoiceItem["subscriptionName"] = "subscriptionName"
        invoiceItem["subscriptionId"] = "subscriptionId"
        invoiceItem["subscriptionNumber"] = "subscriptionNumber"
        invoiceItem
    }
File.open(invoice_preview_filepath, "w"){|f| f.puts(JSON.pretty_generate(invoice_preview)) }
