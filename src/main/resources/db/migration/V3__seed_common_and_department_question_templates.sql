INSERT INTO questions (
    survey_id,
    survey_version_id,
    text,
    type,
    scope,
    department_id,
    metric_code,
    display_order,
    required,
    active
)
SELECT
    sv.survey_id,
    sv.id,
    common.text,
    common.type,
    'COMMON',
    NULL,
    common.metric_code,
    common.display_order,
    common.required,
    true
FROM survey_versions sv
CROSS JOIN (
    VALUES
        ('Personelin iletişiminden memnun kaldınız mı?', 'RATING', 'COMMUNICATION', 10, true),
        ('Size sunulan hizmet hakkında yeterince bilgilendirildiniz mi?', 'RATING', 'INFORMATION', 20, true),
        ('Hizmet sürecinde ihtiyaçlarınıza yeterince özen gösterildi mi?', 'RATING', 'SERVICE_ATTENTION', 30, true),
        ('Genel hizmet deneyiminizi nasıl değerlendirirsiniz?', 'RATING', 'GENERAL_EXPERIENCE', 40, true),
        ('Eklemek istediğiniz görüş veya öneriniz var mı?', 'TEXT', 'GENERAL_COMMENT', 900, false)
) AS common(text, type, metric_code, display_order, required)
WHERE sv.status = 'PUBLISHED'
  AND NOT EXISTS (
      SELECT 1
      FROM questions q
      WHERE q.survey_id = sv.survey_id
        AND q.survey_version_id = sv.id
        AND q.scope = 'COMMON'
        AND q.metric_code = common.metric_code
  );

INSERT INTO questions (
    survey_id,
    survey_version_id,
    text,
    type,
    scope,
    department_id,
    metric_code,
    display_order,
    required,
    active
)
SELECT
    sv.survey_id,
    sv.id,
    template.text,
    'RATING',
    'DEPARTMENT_SPECIFIC',
    d.id,
    template.metric_code,
    template.display_order,
    true,
    true
FROM survey_versions sv
JOIN surveys s ON s.id = sv.survey_id
JOIN departments d ON d.active = true
CROSS JOIN (
    VALUES
        ('ACİL SERVİS', 'İlk müdahale süresi beklentilerinizi karşıladı mı?', 'FIRST_RESPONSE_TIME', 110),
        ('ACİL SERVİS', 'Acil servis süreçlerini nasıl değerlendirirsiniz?', 'EMERGENCY_PROCESS', 120),
        ('POLİKLİNİK', 'Randevu sürecinden memnun kaldınız mı?', 'APPOINTMENT_PROCESS', 110),
        ('POLİKLİNİK', 'Bekleme süresini nasıl değerlendirirsiniz?', 'WAITING_TIME', 120),
        ('POLİKLİNİK', 'Muayene deneyiminizi nasıl değerlendirirsiniz?', 'EXAMINATION_EXPERIENCE', 130),
        ('YATAN HASTA', 'Hemşirelik bakımından memnun kaldınız mı?', 'NURSING_CARE', 110),
        ('YATAN HASTA', 'Oda konforunu nasıl değerlendirirsiniz?', 'ROOM_COMFORT', 120),
        ('YATAN HASTA', 'Temizlikten memnun kaldınız mı?', 'CLEANLINESS', 130),
        ('LABORATUVAR', 'Numune alma sürecini nasıl değerlendirirsiniz?', 'SAMPLE_COLLECTION', 110),
        ('LABORATUVAR', 'Sonuçlara erişim sürecini nasıl değerlendirirsiniz?', 'RESULT_ACCESS', 120),
        ('LABORATUVAR', 'Laboratuvar bekleme süresini nasıl değerlendirirsiniz?', 'WAITING_TIME', 130),
        ('RADYOLOJİ', 'Görüntüleme sürecini nasıl değerlendirirsiniz?', 'IMAGING_PROCESS', 110),
        ('RADYOLOJİ', 'İşlem öncesi bilgilendirmeden memnun kaldınız mı?', 'PROCEDURE_INFORMATION', 120),
        ('RADYOLOJİ', 'Radyoloji bekleme süresini nasıl değerlendirirsiniz?', 'WAITING_TIME', 130)
) AS template(department_name, text, metric_code, display_order)
WHERE sv.status = 'PUBLISHED'
  AND upper(d.name) = template.department_name
  AND NOT EXISTS (
      SELECT 1
      FROM questions q
      WHERE q.survey_id = sv.survey_id
        AND q.survey_version_id = sv.id
        AND q.department_id = d.id
        AND q.metric_code = template.metric_code
  );
