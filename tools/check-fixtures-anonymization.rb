#!/usr/bin/ruby

require 'json'
require 'find'
require 'pathname'

def check_json_file(file_path)
  begin
    content = File.read(file_path)
    data = JSON.parse(content)
    
    if data.key?('gu:sanitised') && data['gu:sanitised'] == true
      return { valid: true, message: nil }
    else
      return { valid: false, message: "gu:sanitised missing or not true" }
    end
  rescue JSON::ParserError => e
    return { valid: false, message: "invalid json, error: #{e.message}" }
  rescue Errno::ENOENT => e
    return { valid: false, message: "file not found, error: #{e.message}" }
  rescue => e
    return { valid: false, message: e.message }
  end
end

repo_root = `git rev-parse --show-toplevel`.strip

target_dir = Pathname.new(repo_root).join('lambda', 'src', 'test', 'resources').to_s

unless Dir.exist?(target_dir)
  puts "error: directory '#{target_dir}' not found!"
  exit 1
end

puts "checking fixtures in #{target_dir}..."

json_files = []
invalid_files = []

Find.find(target_dir) do |path|
  next unless File.file?(path) && path.end_with?('.json')
  json_files << path
end

if json_files.empty?
  exit 0
end

json_files.each do |file_path|
  result = check_json_file(file_path)
  if !result[:valid]
    puts "#{file_path}, error: #{result[:message]}"
    invalid_files << file_path
  end
end

if invalid_files.any?
  exit 1
else
  puts "all json files have 'gu:sanitised' set to true!"
  exit 0
end