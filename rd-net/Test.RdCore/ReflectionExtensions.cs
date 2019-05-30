using System;
using System.Reflection;
using JetBrains.Annotations;

namespace Test.RdCore
{
    public static class ReflectionExtensions
    {
        [CanBeNull]
        private static FieldInfo TryGetField([NotNull] this Type type, [NotNull] string sFieldName)
        {
            var fieldInfo = type.GetField(sFieldName, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic);
            if (fieldInfo != null)
                return fieldInfo;
      
            for (var t = type.BaseType; t != null; t = t.BaseType)
            {
                fieldInfo = t.GetField(sFieldName, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic);
                if (fieldInfo != null)
                    return fieldInfo;
            }

            return null;
        }
        
        public static object GetDynamicField([NotNull] this object obj, [NotNull] string sFieldName)
        {
            if(obj == null)
                throw new ArgumentNullException(nameof(obj));
            if(sFieldName == null)
                throw new ArgumentNullException(nameof(sFieldName));
            var type = obj.GetType();
            FieldInfo field = type.TryGetField(sFieldName);
            if(field == null)
            {
                throw new MissingFieldException(type.ToString(), sFieldName);
                return null;
            }

            return field.GetValue(obj);
        }
        
        [CanBeNull]
        private static PropertyInfo TryGetProperty([NotNull] this Type type, [NotNull] string propertyName)
        {
            var propertyInfo = type.GetProperty(propertyName, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.GetProperty);
            if (propertyInfo != null)
                return propertyInfo;
      
            for (var t = type.BaseType; t != null; t = t.BaseType)
            {
                propertyInfo = t.GetProperty(propertyName, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.GetProperty);
                if (propertyInfo != null)
                    return propertyInfo;
            }

            return null;
        }
        
        public static object GetDynamicProperty([NotNull] this object reflectedObject, [NotNull] string propertyName)
        {
            if(reflectedObject == null)
                throw new ArgumentNullException(nameof(reflectedObject));
            if(propertyName == null)
                throw new ArgumentNullException(nameof(propertyName));
            var type = reflectedObject.GetType();
            var property = type.TryGetProperty(propertyName);
            if(property == null)
            {
                throw new MissingMemberException(type.ToString(), propertyName);
                return null;
            }

            return property.GetValue(reflectedObject, new object[0]);
        }
    }
}