using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml;

namespace JetBrains.Rd.Scripts
{
  internal class Program
  {
    // ReSharper disable once FieldCanBeMadeReadOnly.Global
    // ReSharper disable once MemberCanBePrivate.Global
    public static string RdVersion = "201.20200114.314";
    // ReSharper disable once FieldCanBeMadeReadOnly.Global
    public static string RiderFolderPath = "c:/work/Uber";

    public static string[] Packages = {"JetBrains.Lifetimes", "JetBrains.RdFramework", "JetBrains.RdFramework.Reflection", "JetBrains.RdGen"};
    
    static readonly Type t = typeof(Program);
    static readonly List<MethodInfo> ep = t.GetMethods().Where(mi => mi.IsStatic && mi.IsPublic && mi.ReturnType == typeof(void) && mi.GetParameters().Length == 0).ToList();
    static readonly List<FieldInfo> ps = t.GetFields().Where(f => f.IsStatic && f.IsPublic && f.FieldType == typeof(string)).ToList();

    private static void E(string msg = "") => Console.Error.WriteLine(msg);
    
    private static void Error(string error)
    {
      E(error);
      E();

      
      E($"Usage: {t.Assembly.GetName().Name}.exe <entry-point> <parameter-name>=<parameter-value>");
      E($"Entry points:");
      E("  " + string.Join("\n  ", ep.Select(it => it.Name)));
      E($"Parameters:");
      E("  " + string.Join("\n  ", ps.Select(it => it.Name)));

      Environment.Exit(1);
    } 
    
    public static void Main(string[] args)
    {
      
      if (args.Length == 0)
      {
        Error("Specify entry point.");
      }

      var method = ep.Find(mi => mi.Name.Equals(args[0], StringComparison.OrdinalIgnoreCase));
      if (method == null)
        Error($"Invalid entry point: {args[0]}");

      foreach (var p in args.Skip(1))
      {
        var split = p.Split('=');
        if (split.Length != 2)
          Error($"Illegal parameter notation '{p}': must be '<parameter-name>=<parameter-value>'");

        var pname = split[0];
        var field = ps.Find(it => it.Name.Equals(pname, StringComparison.OrdinalIgnoreCase));
        if (field == null)
          Error($"Invalid parameter name: {pname}");
        
        field?.SetValue(null, split[1]);
      }

      method?.Invoke(null, new object[0]);
    }
    

    public static void UpdateToVersion()
    {
      if (string.IsNullOrEmpty(RiderFolderPath))
        Error($"Parameter {nameof(RiderFolderPath)} is empty");
      
      if (string.IsNullOrEmpty(RdVersion))
        Error($"Parameter {nameof(RdVersion)} is empty");

      var productRoot = RiderFolderPath + "/Product.Root";
      if (!File.Exists(productRoot))
      {
        Error($"Invalid repository root: file `{productRoot}` doesn't exist");
      }


      bool IsPackagesConfig(string file) => file.EndsWith("packages.config", StringComparison.OrdinalIgnoreCase);
      bool IsProj(string file) => file.EndsWith(".csproj", StringComparison.OrdinalIgnoreCase) 
                                  || file.EndsWith(".vbproj", StringComparison.OrdinalIgnoreCase)
                                  || file.EndsWith(".vcxproj", StringComparison.OrdinalIgnoreCase);
      
      IEnumerable<string> FilesToProcess(string folder)
      {
        if (folder.EndsWith("data") && folder.Replace('\\','/').EndsWith("test/data"))
          yield break; //very big folder

        var files = Directory.GetFiles(folder, "*", SearchOption.TopDirectoryOnly)
          .Where(it => IsProj(it) || IsPackagesConfig(it))
          .ToList();

        if (files.Count > 0) 
        {
          foreach (var file in files)
            yield return file;
          
        }
        
        if (!files.Any(IsProj)) //treat proj as terminating file
        {
          var dirs = Directory.GetDirectories(folder, "*", SearchOption.TopDirectoryOnly)
            .SelectMany(FilesToProcess);
             
          foreach (var file in dirs)
            yield return file;
        }
      }

      void UpdatePackagesConfig(string file)
      {
        Console.WriteLine($"Updating: {file}");
        
        EditXmlDocument(file, doc =>
        {
          foreach (var pkg in Packages)
          foreach (XmlNode node in doc.SelectNodes($"/packages/package[@id='{pkg}']"))
          {
            node.Attributes["version"].Value = RdVersion;
          }
        });
      }

      void UpdateCsproj(string file)
      {
        Console.WriteLine($"Updating: {file}");
        EditXmlDocument(file, doc =>
        {
          var namespaceManager = new XmlNamespaceManager(doc.NameTable);
          namespaceManager.AddNamespace("", doc.DocumentElement.NamespaceURI);
          namespaceManager.AddNamespace("x", doc.DocumentElement.NamespaceURI);

          foreach (var pkg in Packages)
          {

            foreach (XmlNode node in doc.SelectNodes($"//x:HintPath[contains(text(), \"{pkg}\")]", namespaceManager))
            {
              var text = (XmlText) node.FirstChild;
              text.Value = Regex.Replace(text.Value, $"{pkg}.*.\\\\lib", $"{pkg}.{RdVersion}\\lib");
            }

            foreach (XmlNode node in doc.SelectNodes($"//x:Import[contains(@Project, \"{pkg}\")]", namespaceManager))
            {
              node.Attributes["Project"].Value = Regex.Replace(node.Attributes["Project"].Value, $"{pkg}.*.\\\\build", $"{pkg}.{RdVersion}\\build");
            }
          }
        });
      }

      var roots = Directory.GetDirectories(RiderFolderPath, "*", SearchOption.TopDirectoryOnly)
        .Where(it => File.Exists(it + "/SubplatformsCollection.Root "));
      

      var sw = new Stopwatch();
      foreach (var root in roots)
      {
        sw.Reset();
        sw.Start();


        foreach (var file in FilesToProcess(root).ToList())
        {
          if (IsPackagesConfig(file))
            UpdatePackagesConfig(file);
          if (IsProj(file))
            UpdateCsproj(file);
          
        }
        
        Console.WriteLine($"{root}: {sw.Elapsed}");
      }
//      var filesToProcess = roots.SelectMany(FilesToProcess);
//      Console.WriteLine(filesToProcess.Count(IsCsproj));
//      Console.WriteLine(filesToProcess.Count(IsPackagesConfig));

    }

    private static void EditXmlDocument(string path, Action<XmlDocument> modify)
    {
      var doc = new XmlDocument();
      Encoding enc;
      using (var reader = new StreamReader(path, new UTF8Encoding(false)))
      {
        doc.Load(reader);
        enc = reader.CurrentEncoding;
        if (enc is UTF8Encoding utf8)
          Console.WriteLine("Preamble length: {0}", utf8.Preamble.Length);
      }

      modify(doc);

      string newLine = LineEndingUtil.Detect(path);
      using (var writer = new StreamWriter(path, false, enc) {NewLine = newLine}) doc.Save(writer);
    }
  }
}