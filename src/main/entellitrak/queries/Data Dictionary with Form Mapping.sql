	select *
	from
	(

	SELECT obj1.table_name AS TABLE_NAME ,
	  obj1.description     AS TABLE_DESC ,
	  obj1.object_name     AS OBJECT_NAME,
	  ''                   AS COLUMN_NAME ,
	 ''     AS DATA_ELEMENT_NAME ,
	   '' as FORM_NAME,
	  ''    AS LABEL_ELEMENT_NAME,
	  ''                   AS COLUMN_DESC ,
	  ''                   AS DATA_TYPE ,
	  0                    AS SORTING_DELETETHIS ,
	  case  when to_char(ele1.lookup_definition_id) is  
	  null then 'No' when to_char(ele1.lookup_definition_id) is not null then 'Yes' end AS LOOKUP ,
	  0 as FORM_DISPLAY_ORDER
	FROM etk_data_object obj1
	JOIN etk_data_element ele1
	ON ele1.data_object_id = obj1.data_object_id
	JOIN
	  (SELECT tracking_config_id
	  FROM etk_tracking_config
	  WHERE config_version =
		(SELECT MAX(config_version) FROM etk_tracking_config
		)
	  ) d
	ON d.tracking_config_id=obj1.tracking_config_id



	UNION 




	SELECT obj2.table_name AS TABLE_NAME ,
	  ''                   AS TABLE_DESC ,
  	  obj2.object_name     AS OBJECT_NAME,
	  ele2.column_name     AS COLUMN_NAME ,
		ele2.name     AS DATA_ELEMENT_NAME ,
		edf.name      AS FORM_NAME,
		efc.label     as DATA_LABEL_NAME,
	  CASE
		WHEN def2.name IS NULL
		THEN ele2.description
		WHEN obj2a.table_name IS NULL
		THEN ele2.description
		  || ' from parent table ['
		  || def2.name
		  || ']'
		ELSE ele2.description
		  || ' from '
		  || obj2a.table_name
	  END AS COLUMN_DESC ,
	  CASE
		WHEN info2.data_type = 'VARCHAR2'
		THEN info2.data_type
		  || '('
		  || trim(TO_CHAR(info2.data_length,'9999'))
		  || ')'
		WHEN info2.data_type IN ('INT','NUMBER')
		THEN info2.data_type
		  || '('
		  || trim(TO_CHAR(info2.data_precision,'999'))
		  || ','
		  || trim(TO_CHAR(info2.data_scale,'999'))
		  || ')'
		ELSE info2.data_type
	  END AS DATA_TYPE ,
	  1   AS SORTING_DELETETHIS ,
	  case  when to_char(ele2.lookup_definition_id) is  
	  null then 'No' when to_char(ele2.lookup_definition_id) is not null then 'Yes' end AS LOOKUP , 
	  efc.display_order as FORM_DISPLAY_ORDER
	FROM etk_data_element ele2
	JOIN etk_data_object obj2
	ON obj2.data_object_id = ele2.data_object_id
	join etk_data_form edf on edf.data_object_id = obj2.data_object_id
	join etk_form_control efc on efc.data_form_id = edf.data_form_id
	join ETK_FORM_CTL_ELEMENT_BINDING elBinding on elBinding.FORM_CONTROL_ID = efc.FORM_CONTROL_ID
				  and elBinding.data_element_id = ele2.data_element_id
	LEFT JOIN etk_lookup_definition def2
	ON ele2.lookup_definition_id = def2.lookup_definition_id
	LEFT JOIN etk_data_object obj2a
	ON def2.data_object_id = obj2a.data_object_id
	JOIN
	  (SELECT tracking_config_id
	  FROM etk_tracking_config
	  WHERE config_version =
		(SELECT MAX(config_version) FROM etk_tracking_config
		)
	  ) d
	ON d.tracking_config_id=obj2.tracking_config_id
	JOIN user_tab_columns info2
	ON info2.column_name    = ele2.column_name
	AND info2.table_name    = obj2.table_name
	WHERE ele2.column_name != 'ID_WORKFLOW'



	UNION




	SELECT ele4.table_name AS TABLE_NAME ,
	  'Mapping Table'      AS TABLE_DESC ,
	 obj4.object_name     AS OBJECT_NAME,
	  ''                   AS COLUMN_NAME ,
		 ele4.name     AS DATA_ELEMENT_NAME ,
		   '' as FORM_NAME,
	  ''     as DATA_LABEL_NAME,
	  ''                   AS COLUMN_DESC ,
	  ''                   AS DATA_TYPE ,
	  0                    AS SORTING_DELETETHIS ,
		case  when to_char(ele4.lookup_definition_id) is  
	  null then 'No' when to_char(ele4.lookup_definition_id) is not null then 'Yes' end AS LOOKUP   ,
	  0 as FORM_DISPLAY_ORDEr
	FROM etk_data_element ele4
	JOIN etk_data_object obj4
	ON obj4.data_object_id = ele4.data_object_id
	JOIN
	  (SELECT tracking_config_id
	  FROM etk_tracking_config
	  WHERE config_version =
		(SELECT MAX(config_version) FROM etk_tracking_config
		)
	  ) d
	ON d.tracking_config_id=obj4.tracking_config_id
	WHERE NVL(ele4.table_name,' ')  != ' '



	UNION



	
	SELECT ele3.table_name AS TABLE_NAME ,
	  ''                   AS TABLE_DESC ,
	  obj3.object_name     AS OBJECT_NAME,
	  info3.column_name    AS COLUMN_NAME ,
		 ele3.name     AS DATA_ELEMENT_NAME ,
		 edf.name      AS FORM_NAME,
			efc.label     as DATA_LABEL_NAME,
	  CASE
		WHEN info3.column_name = 'ID'
		THEN 'Identity value for table record'
		WHEN info3.column_name = 'ID_OWNER'
		THEN 'Data row from child table '
		  || obj3.table_name
		WHEN info3.column_name = 'LIST_ORDER'
		THEN 'Order of appearance in the interface'
		ELSE
		  CASE
			WHEN obj3a.table_name IS NULL
			THEN 'Data row from parent table ['
			  || def3.name
			  || ']'
			ELSE 'Data row from parent table '
			  || obj3a.table_name
		  END
	  END AS COLUMN_DESC ,
	  CASE
		WHEN info3.data_type = 'VARCHAR2'
		THEN info3.data_type
		  || '('
		  || trim(TO_CHAR(info3.data_length,'9999'))
		  || ')'
		WHEN info3.data_type IN ('NUMBER')
		THEN info3.data_type
		  || '('
		  || trim(TO_CHAR(info3.data_precision,'999'))
		  || ','
		  || trim(TO_CHAR(info3.data_scale,'999'))
		  || ')'
		ELSE info3.data_type
	  END AS DATA_TYPE ,
	  1   AS SORTING_DELETETHIS , 
	   case  when to_char(ele3.lookup_definition_id) is  
	  null then 'No' when to_char(ele3.lookup_definition_id) is not null then 'Yes' end AS LOOKUP  ,
	  efc.display_order as FORM_DISPLAY_ORDER
	FROM etk_data_element ele3
	JOIN user_tab_columns info3
	ON info3.table_name = ele3.table_name
	JOIN etk_data_object obj3
	ON ele3.data_object_id = obj3.data_object_id
	JOIN etk_lookup_definition def3
	ON ele3.lookup_definition_id = def3.lookup_definition_id
	LEFT JOIN etk_data_object obj3a
	ON def3.data_object_id = obj3a.data_object_id
	join etk_data_form edf on edf.data_object_id = obj3.data_object_id
	join etk_form_control efc on efc.data_form_id = edf.data_form_id
	join ETK_FORM_CTL_ELEMENT_BINDING elBinding on elBinding.FORM_CONTROL_ID = efc.FORM_CONTROL_ID
				  and elBinding.data_element_id = ele3.data_element_id
	JOIN
	  (SELECT tracking_config_id
	  FROM etk_tracking_config
	  WHERE config_version =
		(SELECT MAX(config_version) FROM etk_tracking_config
		)
	  ) d
	ON d.tracking_config_id=obj3.tracking_config_id
	WHERE NVL(ele3.table_name,' ')  != ' '
	  AND info3.column_name not in ('ID','ID_OWNER', 'LIST_ORDER')
	ORDER BY table_name,
	  SORTING_DELETETHIS
	)
	order by form_name, form_display_order