# CD3
Утилита для парсинга html Ютуба и скачивания комментариев к видео.
Переписываю аналогичный кодна Питоне, пока всё подряд накидываю в Мейне, потом порефакторю. В перспективе будет сервис.

## Алгоритм
Сначала получаем html, оттуда берем конфигурацию, часть которой будем использовать в ajax-запросах

находим эндпойнты, характерные для прокрутки и дальнейшей подгрузки, для каждого (они называются continuations) делаем ajax-запрос и парсим ответ. 
Если в ответе находим другие эндпойнты для следующих ajax-запросов, их тоже добавляем в список continuations

## Текущее состояние:
Вроде работает, но количество выданных комментариев не совпадает со счетчиком,который отображает ЮТ над блоклм комментов. При ручном подсчете вышлорасхождение в 1 коммент, так что скорее всего дело в скрытых или удаленных модератором. Для наших целей это хороший результат.

В исходном скрипте не реализована группировка, то есть если коммент вляется ответом, то непонятно, на что конкретно это ответ. 
Строго говоря, есть 2 вида "ответов" - список ответов к первому 

В выводе есть только boolean reply = true и айдишник коммента, который состоит из двух частей. 
Попробуем проанализировать айдишники, надо составить таблицу

|id оригинала               | id ответа                                         | текст для поиска
+---------------------------+---------------------------------------------------+----------------------------------------------
|Ugz25cqFRKgIIVYpzZp4AaABAg |                                                   |Земля плоская! Электричества не существует!
|                           |Ugz25cqFRKgIIVYpzZp4AaABAg.A547Lcuvd9AA548QuyCbBn  |Три слона уже устали
|                           |Ugz25cqFRKgIIVYpzZp4AaABAg.A547Lcuvd9AA549Chp7qJJ  |Настоящий бог Колбас!

|UgwSZa9HUSBkMsloDBh4AaABAg |                                                   |Тргда может бахнем ?
|                           |UgwSZa9HUSBkMsloDBh4AaABAg.A544MKu4LeBA549aCz_x0K  |Может и бахнем , но позже

|UgyhTnBOZUlAKdKPKhV4AaABAg |                                                   |Посмотрим какой это фейк, когда бахнут
|                           |UgyhTnBOZUlAKdKPKhV4AaABAg.A543zZxY-FlA54ErR7cyE2  |Не бахнут,ато фейк расскроется

Ну, тут всё очевидно: root-комментарий имеет короткий id, а ответы на него имеют такой же id, но с добавлением точки и дополнительного айдишника. Напрашивается отличный простой способ отобразить ветки ответов. Добавим в наш вывод поля reply и id, и отсортируем по ним конечный список, для этого напишем анонимный компаратор по двум пропертям - первая часть айдишника , и порядковый номер в изначальном списке выдачи. 

## Улучшение кода
На даннный момент скорость оставляет желать лучшего :/ Ёще бы - гоняем туда-сюда json'ы по нескольку раз. По-хорошему, надо выделить сразу все константы из html и первоначальные данные, оптимизировать поиск по json (особенно ответ на ajax-запрос парсится много раз), потом переписать всё на Мапы, но начнём с другого. Чтобы было удобнее улучшать код, сперва порефакторим. Очень хочется, раз уж пишем новый класс, написать всё сразу правильно и оптимизированно, но надо быть последовательными, и не сломать код, пока не написаны тесты. Так что, скрепя сердце, вооружимся Ctrl+C и Ctrl+V...., ограничась небольшими модификациями.

Вместо статического main сделаем "умный" объект Downloader, и начнем рефакторить с двух концов.

На конце Downloader'а сделаем дата-класс Comment. Поля нам уже известны - пoрядковый номер в нашем алгоритме (который выдает их по новизне, вроде бы), имя автора, текст комментария, isReply и commentId. в итерациях по commentEntityPayload будем добавлять в итоговый лист новые объекты с заполненными полями. На начале - переместим все подготовительные операции в конструктор, да передадим в него произвольный url. Экстрактим методы, сложное пока не трогаем. Переместим кое-что кое-куда. Добавим таймер в main().

25 секунд, много! будем оптимизировать. 
