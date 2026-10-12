package com.naury.chageun.data.weather

import java.io.File

/** 앱에 넣은 목록을 바탕으로 웹에 올라올 법한 새 목록과 안내 파일을 만든다. */
internal object RegionListFixtures {

    val bundledBytes: ByteArray by lazy { File("src/main/assets/${KoreaRegionCsv.ASSET_PATH}").readBytes() }

    val bundledRowCount: Int by lazy { KoreaRegionListCheck.parse(bundledBytes).size }

    /** 역삼1동(1168064000)의 코드만 1168064100으로 바꾼 목록. */
    val renamedBytes: ByteArray by lazy {
        bundledBytes.toString(Charsets.UTF_8).replace("1168064000,역삼1동", "1168064100,역삼1동").toByteArray()
    }

    fun manifest(
        bytes: ByteArray = renamedBytes,
        version: String = "2026-10",
        file: String = "regions-$version.csv",
        rows: Int = bundledRowCount,
        renamedCodes: Map<String, String> = mapOf("1168064000" to "1168064100"),
        sha256: String = KoreaRegionListCheck.sha256(bytes),
    ) = KoreaRegionManifest(
        schema = KoreaRegionManifest.SCHEMA,
        version = version,
        file = file,
        sha256 = sha256,
        rows = rows,
        source = "기상청 단기예보 조회서비스 격자·위경도 (공공데이터포털 15084084), 2026-10-01, 저작자표시",
        renamedCodes = renamedCodes,
    )
}
