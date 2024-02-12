# encoding: UTF-8

require 'json'

def ratePlanDate(rateplan)
    rateplan["ratePlanCharges"].first["originalOrderDate"]
end

subscription = JSON.parse(IO.read("subscription.json"))

puts "number of rate plans: #{subscription["ratePlans"].size}"

puts "date distribution:"
subscription["ratePlans"].each_with_index{|rp, i| puts "    #{i}: #{ratePlanDate(rp)}" }

puts "lastChangeType distribution:"
subscription["ratePlans"].each_with_index{|rp, i| puts "    #{i}: #{rp["lastChangeType"]}" }

puts "number of rate plans with a date: #{subscription["ratePlans"].select{|rp| ratePlanDate(rp) }.size}"